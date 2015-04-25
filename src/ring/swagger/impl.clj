(ns ring.swagger.impl
  (:require [schema.core :as s]))

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
