(ns ring.swagger.purge
  (:require [schema.core :as s]
            [clojure.string :as st]
            [camel-snake-kebab :refer [->CamelCase]]))

(defn- valid-container? [x]
  (and
    (or (sequential? x)
        (set? x))
    (= (count x) 1)))

(defn- plain-map? [x]
  (and (instance? clojure.lang.APersistentMap x) ;; need to filter out Schema records
       (not (s/schema-name x)))) ;; and named schemas

(defn- root? [keys] (not (butlast keys)))

(defn- full-name [path]
  (->> path
       (remove number?)
       (map name)
       (map ->CamelCase)
       (apply str)
       symbol))

(defn with-sub-schemas [schema]
  (let [schemas (atom {})]
    (letfn [
            (collect-schemas [schema keys]
              (cond

                ;; root or anonoymous map
                (or (root? keys) (plain-map? schema))
                (do
                  (swap! schemas assoc keys schema)
                  (doseq [[k v] schema]
                    (collect-schemas v (conj keys k))))

                ;; valid container
                (valid-container? schema)
                (collect-schemas (first schema) (conj keys 0))))]
      (collect-schemas schema [(s/schema-name schema)])
      (reduce (fn [schema [[_ & keys :as all-keys] _]]
                (if-not (seq keys)
                  schema
                  (update-in schema keys
                             (fn [subschema]
                               (if-not subschema
                                 subschema
                                 (with-meta subschema {:name (full-name all-keys)}))))))
              schema
              @schemas))))

;;
;; Spiking
;;

(s/defschema OrderType {:name String})

(s/defschema Order {:id String
                    :name String
                    :type OrderType
                    :mass [{:unit (s/enum :kg :g)
                            :value Long}]
                    :address {:street String
                              :zip Long
                              :country (s/enum :fi :po)
                              :type {:a :b}}})

(def Order2 (with-sub-schemas Order))

(-> Order2 (s/schema-name))
(-> Order2 :mass first (s/schema-name))
(-> Order2 :address :type (s/schema-name))
(clojure.pprint/pprint Order2)

