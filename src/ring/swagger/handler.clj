(ns ring.swagger.handler)

(defn handler [meta f]
  (with-meta f meta))
