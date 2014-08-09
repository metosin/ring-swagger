(ns ring.swagger.json-schema
  (:require [schema.core :as s]
            [ring.swagger.schema :as schema]
            [ring.swagger.data :as data]))

;;
;; Json Schema transformations
;;

(declare json-type)

(defn ->json
  [x & {:keys [top] :or {top false}}]
  (letfn [(type-of [x] (json-type (or (schema/type-map x) x)))]
    (cond
      (nil? x)        {:type "void"}
      (sequential? x) {:type "array"
                       :items (type-of (first x))}
      (set? x)        {:type "array"
                       :uniqueItems true
                       :items (type-of (first x))}
      :else           (if top
                        (if-let [schema-name (s/schema-name x)]
                          {:type schema-name}
                          (or (type-of x) {:type "void"}))
                        (type-of x)))))

;;
;; dispatch
;;

(defmulti json-type identity)
(defmulti json-type-class (fn [e] (class e)))

;;
;; identity-based dispatch
;;

(defmethod json-type data/Long*     [_] {:type "integer" :format "int64"})
(defmethod json-type data/Double*   [_] {:type "number" :format "double"})
(defmethod json-type data/String*   [_] {:type "string"})
(defmethod json-type data/Boolean*  [_] {:type "boolean"})
(defmethod json-type data/Keyword*  [_] {:type "string"})
(defmethod json-type data/DateTime* [_] {:type "string" :format "date-time"})
(defmethod json-type data/Date*     [_] {:type "string" :format "date"})
(defmethod json-type data/UUID*     [_] {:type "string" :format "uuid"})
(defmethod json-type s/Any          [_] nil)

(defmethod json-type :default [e]
  (or
    (json-type-class e)
    (if (s/schema-name e)
      {:$ref (s/schema-name e)}
      (throw (IllegalArgumentException. (str "don't know how to create json-type of: " e))))))

;;
;; class-based dispatch
;;

(defmethod json-type-class schema.core.EnumSchema [e] (merge (->json (class (first (:vs e)))) {:enum (seq (:vs e))}))
(defmethod json-type-class schema.core.Maybe      [e] (->json (:schema e)))
(defmethod json-type-class schema.core.Both       [e] (->json (first (:schemas e))))
(defmethod json-type-class schema.core.Recursive  [e] (->json (:derefable e)))
(defmethod json-type-class schema.core.EqSchema   [e] (->json (class (:v e))))
(defmethod json-type-class :default [e])

;;
;; Schema -> Json Schema
;;

(defn not-predicate? [x]
  (not= (class x) schema.core.Predicate))

(defn properties [schema]
  (into {}
        (for [[k v] schema
              :when (not-predicate? k)
              :let [k (s/explicit-schema-key k)
                    v (merge
                        (dissoc (meta v) :model :name)
                        (try (->json v)
                             (catch Exception e
                               (throw
                                 (IllegalArgumentException.
                                   (str "error converting to json schema [" k " " (s/explain v) "]") e)))))]]
          (and v [k v]))))
