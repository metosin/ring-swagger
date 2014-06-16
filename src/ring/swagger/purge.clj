(ns ring.swagger.purge
  (:require [schema.core :as s]
            [clojure.string :as st]
            [ring.swagger.impl :refer :all]
            [ring.swagger.schema :refer :all]
            [camel-snake-kebab :refer [->CamelCase]]))

(defn with-sub-schemas [schema]
  (letfn [(full-name [path]
            (->> path (map name) (map ->CamelCase) (apply str) symbol))
          (collect-schemas [keys schema]
            (cond
              (map? schema)
              (if (and (seq (pop keys)) (named-schema? schema))
                schema
                (with-meta
                  (merge {}
                         (into {}
                               (for [[k v] schema
                                     :let [keys (conj keys k)]]
                                 [k (collect-schemas keys v)])))
                  {:name (full-name keys)}))

              (valid-container? schema)
              (contain schema (collect-schemas keys (first schema)))

              :else schema))]
    (collect-schemas [(or (s/schema-name schema)
                          (gensym "schema"))] schema)))


(def Order {:customer {:name String
                       :address {:street String
                                 :city {:name String}}}
            :lines [{:id String
                     :tags #{{:name String}}}]})

(def Order2 (with-sub-schemas Order))

(-> Order2 :customer :address :city s/schema-name)
(-> Order2 :lines first :tags first s/schema-name)
