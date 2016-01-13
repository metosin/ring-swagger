(ns ring.swagger.json-schema-test
  (:require [midje.sweet :refer :all]
            [schema.core :as s]
            [plumbing.core :as p]
            [plumbing.fnk.pfnk :as pfnk]
            [ring.swagger.json-schema :refer :all]
            [ring.swagger.core :refer [with-named-sub-schemas]]
            [linked.core :as linked])
  (:import [java.util Date UUID]
           [org.joda.time DateTime LocalDate]
           [java.util.regex Pattern]
           [clojure.lang Symbol]))

(s/defschema Model {:value String})

(facts "type transformations"
  (facts "java types"
    (->swagger Integer)   => {:type "integer" :format "int32"}
    (->swagger Long)      => {:type "integer" :format "int64"}
    (->swagger Double)    => {:type "number" :format "double"}
    (->swagger Number)    => {:type "number" :format "double"}
    (->swagger Symbol)    => {:type "string"}
    (->swagger String)    => {:type "string"}
    (->swagger Boolean)   => {:type "boolean"}
    (->swagger Date)      => {:type "string" :format "date-time"}
    (->swagger DateTime)  => {:type "string" :format "date-time"}
    (->swagger LocalDate) => {:type "string" :format "date"}
    (->swagger Pattern)   => {:type "string" :format "regex"}
    (->swagger #"[6-9]")  => {:type "string" :pattern "[6-9]"}
    (->swagger UUID)      => {:type "string" :format "uuid"})

  (fact "schema types"
    (->swagger s/Int)     => {:type "integer" :format "int64"}
    (->swagger s/Str)     => {:type "string"}
    (->swagger s/Symbol)  => {:type "string"}
    (->swagger s/Inst)    => {:type "string" :format "date-time"}
    (->swagger s/Num)     => {:type "number" :format "double"})

  (fact "containers"
    (->swagger [Long])    => {:type "array" :items {:format "int64" :type "integer"}}
    (->swagger #{Long})   => {:type "array" :items {:format "int64" :type "integer"} :uniqueItems true})

  (facts "nil"
    (->swagger nil)       => nil)

  (facts "unknowns"
    (fact "throw exception by default"
      (->swagger java.util.Vector) => (throws IllegalArgumentException))
    (fact "are ignored with *ignore-missing-mappings*"
      (binding [*ignore-missing-mappings* true]
        (->swagger java.util.Vector)) => nil))

  (facts "models"
    (->swagger Model) => {:$ref "#/definitions/Model"}
    (->swagger [Model]) => {:items {:$ref "#/definitions/Model"}, :type "array"}
    (->swagger #{Model}) => {:items {:$ref "#/definitions/Model"}, :type "array" :uniqueItems true})

  (fact "schema predicates"
    (fact "s/enum"
      (->swagger (s/enum :kikka :kakka)) => {:type "string" :enum [:kikka :kakka]}
      (->swagger (s/enum 1 2 3))         => {:type "integer" :format "int64" :enum (seq #{1 2 3})})

    (fact "s/maybe"
      (fact "uses wrapped value by default"
        (->swagger (s/maybe Long)) => (->swagger Long))
      (fact "adds allowEmptyValue when for query and formData as defined by the spec"
        (->swagger (s/maybe Long) {:in :query}) => (assoc (->swagger Long) :allowEmptyValue true)
        (->swagger (s/maybe Long) {:in :formData}) => (assoc (->swagger Long) :allowEmptyValue true))
      (fact "uses wrapped value for other parameters"
        (->swagger (s/maybe Long) {:in :body}) => (->swagger Long)
        (->swagger (s/maybe Long) {:in :header}) => (->swagger Long)
        (->swagger (s/maybe Long) {:in :path}) => (->swagger Long)))

    (fact "s/both -> type of the first element"
      (->swagger (s/both Long String))   => (->swagger Long))

    (fact "s/either -> type of the first element"
      (->swagger (s/either Long String)) => (->swagger Long))

    (fact "s/named -> type of schema"
      (->swagger (s/named Long "long"))  => (->swagger Long))

    (fact "s/one -> type of schema"
      (->swagger [(s/one Long "s")])  => (->swagger [Long]))

    (fact "s/recursive -> type of internal schema"
      (->swagger (s/recursive #'Model))  => (->swagger #'Model))

    (fact "s/eq -> type of class of value"
      (->swagger (s/eq "kikka"))         => (->swagger String))

    (fact "s/Any"
      (fact "defaults to nil"
        (->swagger s/Any)                 => nil
        (->swagger s/Any {:in :body})     => nil
        (->swagger s/Any {:in :header})   => {:type "string"}
        (->swagger s/Any {:in :path})     => {:type "string"}
        (->swagger s/Any {:in :query})    => {:type "string", :allowEmptyValue true}
        (->swagger s/Any {:in :formData}) => {:type "string", :allowEmptyValue true}))

    (fact "s/conditional"
      (->swagger (s/conditional (constantly true) Long (constantly false) String))
      => {:type "void" :oneOf [(->swagger Long) (->swagger String)]}

      (fact "invalid values are removed"
        (->swagger (s/conditional (constantly true) Long (constantly false) s/Any))
        => {:type "void" :oneOf [(->swagger Long)]}))

    (fact "s/constrained"
      (->swagger (s/constrained Long even?))
      => (->swagger Long))

    (fact "s/if"
      (->swagger (s/if (constantly true) Long String))
      => {:type "void" :oneOf [(->swagger Long) (->swagger String)]})

    (fact "s/cond-pre"
      (->swagger (s/cond-pre Model [s/Str]))
      => {:type "void" :oneOf [(->swagger Model) (->swagger [s/Str])]})
    ))

(fact "Optional-key default metadata"
  (properties {(with-meta (s/optional-key :foo) {:default "bar"}) s/Str})
  => {:foo {:type "string" :default "bar"}}

  (fact "nil default is ignored"
    (properties {(with-meta (s/optional-key :foo) {:default nil}) s/Str})
    => {:foo {:type "string"}})

  (fact "pfnk schema"
    (properties (pfnk/input-schema (p/fnk [{x :- s/Str "foo"}])))
    => {:x {:type "string" :default "foo"}})

  (fact "pfnk schema - nil default is ignored"
    (properties (pfnk/input-schema (p/fnk [{x :- s/Str nil}])))
    => {:x {:type "string"}}))

(fact "Describe"
  (tabular
    (fact "Basic classes"
      (let [schema (describe ?class ..desc.. :minimum ..val..)]
        (json-schema-meta schema) => {:description ..desc.. :minimum ..val..}
        (->swagger schema) => (contains {:description ..desc..})))
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
      (->swagger schema) => (contains {:description ..desc..}))))

(fact "leiPredicate evaluation"
  (tabular
    (fact "Schema core predicates"
      (->swagger ?pred) => (->swagger (eval ?pred)))
    ?pred
    s/Str
    s/Bool
    s/Num
    s/Int
    s/Keyword
    s/Symbol
    s/Regex
    s/Inst
    s/Uuid))

(facts "properties"
  (fact "s/Any -values are ignored"
    (keys (properties {:a s/Str
                       :b s/Any}))
    => [:a])

  (fact "s/Keyword -keys are ignored"
    (keys (properties {:a s/Str
                       s/Keyword s/Str}))
    => [:a])

  (fact "Class -keys are ignored"
    (keys (properties {:a s/Str
                       s/Str s/Str}))
    => [:a])

  (fact "Required keyword-keys are used"
    (keys (properties {:a s/Str
                       (s/required-key :b) s/Str}))
    => [:a :b])

  (fact "Required non-keyword-keys are ignored"
    (keys (properties {:a s/Str
                       (s/required-key "b") s/Any}))
    => [:a])

  (fact "s/Any -keys are ignored"
    (keys (properties {:a s/Str
                       s/Any s/Str}))
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
    (keys (properties (linked/map :a String
                                   :b String
                                   :c String
                                   :d String
                                   :e String
                                   :f String
                                   :g String
                                   :h String)))
    => [:a :b :c :d :e :f :g :h])

  (fact "Ordered-map works with sub-schemas"
    (properties (with-named-sub-schemas (linked/map :a String
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

(facts "additional-properties"
  (fact "No additional properties"
    (additional-properties {:a s/Str})
    => nil)

  (fact "s/Keyword"
    (additional-properties {s/Keyword s/Bool})
    => {:type "boolean"})

  (fact "s/Any"
    (additional-properties {s/Any s/Str})
    => {:type "string"})

  (fact "s/Str"
    (additional-properties {s/Str s/Bool})
    => {:type "boolean"})

  (fact "s/Int"
    (additional-properties {s/Int s/Str})
    => {:type "string"}))
