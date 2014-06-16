(ns ring.swagger.purge
  (:require [schema.core :as s]
            [clojure.string :as st]
            [camel-snake-kebab :refer [->CamelCase]]))

(defn contain [x y]
  (cond
    (set? x) (-> x (disj (first x)) (conj y))
    (sequential? x) (-> x pop (conj y))))

(defn valid-container? [x]
  (and
    (or (sequential? x)
        (set? x))
    (= (count x) 1)))

(defn named-schema? [x]
  (boolean (s/schema-name x)))

(defn plain-map? [x]
  (instance? clojure.lang.APersistentMap x))

(defn root? [keys] (not (butlast keys)))

(defn- full-name [path]
  (->> path
       (map name)
       (map ->CamelCase)
       (apply str)
       symbol))

(defn collect-schemas [keys schema]
  (cond

    (map? schema)
    (if (and (not (root? keys)) (named-schema? schema))
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

    :else schema))

(defn with-sub-schemas [schema]
  (collect-schemas [(s/schema-name schema)] schema))
