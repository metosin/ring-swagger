(ns ring.swagger.swagger2-schema
  "Schemas that Ring-Swagger expects from it's clients"
  (require [schema.core :as s]))

(defn opt [x] (s/optional-key x))

(s/defschema Responses
  s/Any
  #_{(s/either (s/eq :default) s/Int) (s/maybe {:s s/Str})
   #"x-" {:t s/Str}})

(s/defschema Parameters
  {(opt :body) s/Any
   (opt :query) s/Any
   (opt :path) s/Any
   (opt :header) s/Any
   (opt :formData) s/Any})

(s/defschema Operation
  {(opt :parameters) Parameters
   (opt :responses) Responses
   s/Keyword s/Any})

(s/defschema Swagger
  {(opt :paths) {s/Str {s/Keyword (s/maybe Operation)}}
   s/Keyword s/Any})
