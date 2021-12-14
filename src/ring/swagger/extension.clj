(ns ring.swagger.extension)

(defmacro defextension
  "Define a wrapper that will conditionally execute the body based on the
  value of test."
  [name test]
  `(defmacro ~name [& body#]
     (when ~test
       `(do ~@body#))))

(defextension java-time
  (every? resolve
          '[java.time.Instant
            java.time.LocalDate
            java.time.LocalTime]))
