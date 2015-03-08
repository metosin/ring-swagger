(ns ring.swagger.impl.walk
  (:require [schema.core :as s]))

(defprotocol WalkableSchema
  (-walk [this inner outer]))

(defn walk
  [this inner outer]
  (if (satisfies? WalkableSchema this)
    (-walk this inner outer)
    this))

(extend-protocol WalkableSchema
  clojure.lang.IMapEntry
  (-walk [this inner outer]
    (outer (vec (inner (key this)) (inner (val this)))))

  clojure.lang.IPersistentMap
  (-walk [this inner outer]
    (if-not (record? this)
      (outer (with-meta
               (into (empty this) (map inner this))
               (meta this)))
      this))

  clojure.lang.IPersistentVector
  (-walk [this inner outer]
    (outer (with-meta (mapv inner this) (meta this))))

  clojure.lang.IPersistentSet
  (-walk [this inner outer]
    (outer (with-meta (into (empty this) (map inner this)) (meta this))))

  schema.core.Maybe
  (-walk [this inner outer]
    (outer (with-meta (s/maybe (inner (:schema this))) (meta this))))

  schema.core.Both
  (-walk [this inner outer]
    (outer (with-meta (apply s/both (map inner (:schemas this))) (meta this))))

  schema.core.Either
  (-walk [this inner outer]
    (outer (with-meta (apply s/either (map inner (:schemas this))) (meta this))))

  schema.core.Recursive
  (-walk [this inner outer]
    (outer (with-meta (s/recursive (inner (:derefable this))) (meta this))))

  schema.core.NamedSchema
  (-walk [this inner outer]
    (outer (with-meta (s/named (inner (:schema this)) (:name this)) (meta this)))))
