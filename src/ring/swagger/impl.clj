(ns ring.swagger.impl
  (:require [schema.core :as s]))

;;
;; Schema containers
;;

(defn asserted-container [x]
  (assert (= (count x) 1) "schema containers can only one element.") x)

(defn contain [x y]
  (cond
    (set? x) (-> x (disj (first x)) (conj y))
    (sequential? x) (-> x pop (conj y))))

(defn valid-container? [x]
  (and (or (sequential? x) (set? x))
       (asserted-container x)))

; TODO: still needed?
(defn update-schema [x f]
  (if (valid-container? x)
    (contain x (f (first x)))
    (f x)))

;;
;; Other
;;

(defn required-keys [schema]
  (filterv s/required-key? (keys schema)))

(defn strict-schema
  "removes open keys from schema"
  [schema]
  {:pre [(map? schema)]}
  (dissoc schema s/Keyword))

(defn loose-schema
  "add open keys for top level schema"
  [schema]
  {:pre [(map? schema)]}
  (assoc schema s/Keyword s/Any))
