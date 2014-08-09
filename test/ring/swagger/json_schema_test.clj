(ns ring.swagger.json-schema-test
  (:require [midje.sweet :refer :all]
            [schema.core :as s]
            [ring.swagger.data :refer :all]
            [ring.swagger.json-schema :refer :all])
  (:import [java.util Date UUID]
           [org.joda.time DateTime LocalDate]))

(s/defschema Model {:value String})

(facts "type transformations"

  (facts "java types"
    (->json Long) => {:type "integer" :format "int64"}
    (->json Double) => {:type "number" :format "double"}
    (->json String) => {:type "string"}
    (->json Boolean) => {:type "boolean"}
    (->json Date) => {:type "string" :format "date-time"}
    (->json DateTime) => {:type "string" :format "date-time"}
    (->json LocalDate) => {:type "string" :format "date"}
    (->json UUID) => {:type "string" :format "uuid"})

  (facts "datatypes"
    (->json Long*) => {:type "integer" :format "int64"}
    (->json Double*) => {:type "number" :format "double"}
    (->json String*) => {:type "string"}
    (->json Boolean*) => {:type "boolean"}
    (->json DateTime*) => {:type "string" :format "date-time"}
    (->json Date*) => {:type "string" :format "date"}
    (->json UUID*) => {:type "string" :format "uuid"})

  (fact "schema types"
    (->json s/Int) => {:type "integer" :format "int64"}
    (->json s/Str) => {:type "string"})

  (fact "containers"
    (->json [Long]) => {:type "array" :items {:format "int64" :type "integer"}}
    (->json #{Long}) => {:type "array" :items {:format "int64" :type "integer"} :uniqueItems true})

  (facts "nil"
    (->json nil) => {:type "void"})

  (fact "special predicates"

    (fact "s/enum"
      (->json (s/enum :kikka :kakka)) => {:type "string" :enum [:kikka :kakka]}
      (->json (s/enum 1 2 3)) => {:type "integer" :format "int64" :enum (seq #{1 2 3})})

    (fact "s/maybe -> type of internal schema"
      (->json (s/maybe Long)) => (->json Long))

    (fact "s/both -> type of the first element"
      (->json (s/both Long String)) => (->json Long))

    (fact "s/recursive -> type of internal schema"
      (->json (s/recursive #'Model)) => (->json #'Model))

    (fact "s/eq -> type of class of value"
      (->json (s/eq "kikka")) => (->json String))

    (fact "top level ->json"
      (s/defschema TopModel {:name String})
      (fact "for named schema"
        (->json TopModel :top true) => {:type 'TopModel})
      (fact "for non-named schema"
        (->json s/Any :top true) => {:type "void"}))))

(facts "generating return types from models, list & set of models"
  (fact "returning Model"
    (->json Model :top true) => {:type 'Model})
  (fact "returning [Model]"
    (->json [Model] :top true) => {:items {:$ref 'Model}, :type "array"})
  (fact "returning #{Model}"
    (->json #{Model} :top true) => {:items {:$ref 'Model}, :type "array" :uniqueItems true}))

(facts "properties"
  (fact "s/Any -values are ignored"
    (keys (properties {:a String
                       :b s/Any})) => [:a]
  (fact "s/Keyword -keys are ignored"
    (keys (properties {:a String
                       s/Keyword s/Any})) => [:a])))
