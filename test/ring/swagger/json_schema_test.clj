(ns ring.swagger.json-schema-test
  (:require [midje.sweet :refer :all]
            [schema.core :as s]
            [ring.swagger.json-schema :refer :all]
            [ring.swagger.core :refer [with-named-sub-schemas]]
            [flatland.ordered.map :refer :all])
  (:import [java.util Date UUID]
           [org.joda.time DateTime LocalDate]
           [java.util.regex Pattern]))

(s/defschema Model {:value String})

(facts "type transformations"
  (facts "java types"
    (->json Integer)   => {:type "integer" :format "int32"}
    (->json Long)      => {:type "integer" :format "int64"}
    (->json Double)    => {:type "number" :format "double"}
    (->json Number)    => {:type "number" :format "double"}
    (->json String)    => {:type "string"}
    (->json Boolean)   => {:type "boolean"}
    (->json Date)      => {:type "string" :format "date-time"}
    (->json DateTime)  => {:type "string" :format "date-time"}
    (->json LocalDate) => {:type "string" :format "date"}
    (->json Pattern)   => {:type "string" :format "regex"}
    (->json #"[6-9]")  => {:type "string" :pattern "[6-9]"}
    (->json UUID)      => {:type "string" :format "uuid"})

  (fact "schema types"
    (->json s/Int)     => {:type "integer" :format "int64"}
    (->json s/Str)     => {:type "string"}
    (->json s/Num)     => {:type "number" :format "double"})

  (fact "containers"
    (->json [Long])    => {:type "array" :items {:format "int64" :type "integer"}}
    (->json #{Long})   => {:type "array" :items {:format "int64" :type "integer"} :uniqueItems true})

  (facts "nil"
    (->json nil)       => {:type "void"})

  (facts "unknowns"
    (fact "throw exception by default"
      (->json java.util.Vector) => (throws IllegalArgumentException))
    (fact "are ignored with *ignore-missing-mappings*"
      (binding [*ignore-missing-mappings* true]
        (->json java.util.Vector)) => nil))

  (fact "schema predicates"
    (fact "s/enum"
      (->json (s/enum :kikka :kakka)) => {:type "string" :enum [:kikka :kakka]}
      (->json (s/enum 1 2 3))         => {:type "integer" :format "int64" :enum (seq #{1 2 3})})

    (fact "s/maybe -> type of internal schema"
      (->json (s/maybe Long))         => (->json Long))

    (fact "s/both -> type of the first element"
      (->json (s/both Long String))   => (->json Long))

    (fact "s/either -> type of the first element"
      (->json (s/either Long String))   => (->json Long))

    (fact "s/named -> type of schema"
      (->json (s/named Long "long"))  => (->json Long))

    (fact "s/recursive -> type of internal schema"
      (->json (s/recursive #'Model))  => (->json #'Model))

    (fact "s/eq -> type of class of value"
      (->json (s/eq "kikka"))         => (->json String))

    (fact "s/Any -> nil"
      (->json s/Any) => nil)))

(facts "generating return types from models, list & set of models"
  (fact "non-named schema"
    (->json s/Any :top true)    => {:type "void"})
  (fact "returning Model"
    (->json Model :top true)    => {:type 'Model})
  (fact "returning [Model]"
    (->json [Model] :top true)  => {:items {:$ref 'Model}, :type "array"})
  (fact "returning #{Model}"
    (->json #{Model} :top true) => {:items {:$ref 'Model}, :type "array" :uniqueItems true}))

(fact "Describe"
  (tabular
    (fact "Basic classes"
      (let [schema (describe ?class ..desc.. :minimum ..val..)]
        (json-schema-meta schema) => {:description ..desc.. :minimum ..val..}
        (->json schema) => (contains {:description ..desc..})))
    ?class
    Long
    Double
    String
    Boolean
    Date
    DateTime
    LocalDate
    Pattern
    UUID
    clojure.lang.Keyword)

  (fact "Describe Model"
    (let [schema (describe Model ..desc..)]
      (json-schema-meta schema) => {:description ..desc..}
      (->json schema) => (contains {:description ..desc..})
      )))

(facts "properties"
  (fact "s/Any -values are ignored"
    (keys (properties {:a String
                       :b s/Any}))
    => [:a])

  (fact "s/Keyword -keys are ignored"
    (keys (properties {:a String
                       s/Keyword s/Any}))
    => [:a])

  (fact "with unknown mappings"
    (fact "by default, exception is thrown"
      (properties {:a String
                   :b java.util.Vector}) => (throws IllegalArgumentException))
    (fact "unknown fields are ignored ig *ignore-missing-mappings* is set"
      (binding [*ignore-missing-mappings* true]
        (keys (properties {:a String
                           :b java.util.Vector})) => [:a])))

  (fact "Keeps the order of properties intact"
    (keys (properties (ordered-map :a String
                                   :b String
                                   :c String
                                   :d String
                                   :e String
                                   :f String
                                   :g String
                                   :h String)))
    => [:a :b :c :d :e :f :g :h])

  (fact "Ordered-map works with sub-schemas"
    (properties (with-named-sub-schemas (ordered-map :a String
                                                     :b {:foo String}
                                                     :c [{:bar String}])))
    => anything)

  (fact "referenced record-schemas"
    (s/defschema Foo (s/enum :one :two))
    (s/defschema Bar {:key Foo})

    (fact "can't get properties out of record schemas"
      (properties Foo)) => (throws AssertionError)

    (fact "nested properties work ok"
      (keys (properties Bar)) => [:key])))
