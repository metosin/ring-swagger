(ns ring.swagger.json-schema-test
  (:require [midje.sweet :refer :all]
            [schema.core :as s]
            [plumbing.core :as p]
            [plumbing.fnk.pfnk :as pfnk]
            [ring.swagger.json-schema :as rsjs]
            [ring.swagger.core :as rsc]
            [ring.swagger.extension :as extension]
            [linked.core :as linked])
  (:import [java.util Date UUID Currency]
           [org.joda.time DateTime LocalDate LocalTime]
           [java.util.regex Pattern]
           [clojure.lang Symbol]
           (java.io File)))

(s/defschema Model {:value String})

(s/defrecord Keyboard [type :- (s/enum :left :right)])
(s/defrecord User [age :- s/Int, keyboard :- Keyboard])

;; Make currency return nil for testing purporses
(defmethod rsjs/convert-class java.util.Currency [_ _ _] nil)

(facts "type transformations"
  (fact "mapped to nil"
    (rsjs/->swagger Currency) => nil)

  (facts "java types"
    (rsjs/->swagger Integer) => {:type "integer" :format "int32"}
    (rsjs/->swagger Long) => {:type "integer" :format "int64"}
    (rsjs/->swagger Double) => {:type "number" :format "double"}
    (rsjs/->swagger Number) => {:type "number" :format "double"}
    (rsjs/->swagger Symbol) => {:type "string"}
    (rsjs/->swagger String) => {:type "string"}
    (rsjs/->swagger Boolean) => {:type "boolean"}
    (rsjs/->swagger Date) => {:type "string" :format "date-time"}
    (rsjs/->swagger DateTime) => {:type "string" :format "date-time"}
    (rsjs/->swagger LocalDate) => {:type "string" :format "date"}
    (rsjs/->swagger LocalTime) => {:type "string" :format "time"}
    (rsjs/->swagger Pattern) => {:type "string" :format "regex"}
    (rsjs/->swagger #"[6-9]") => {:type "string" :pattern "[6-9]"}
    (rsjs/->swagger UUID) => {:type "string" :format "uuid"}
    (extension/java-time
    (rsjs/->swagger File) => {:type "file"}
      (rsjs/->swagger java.time.Instant) => {:type "string" :format "date-time"}
      (rsjs/->swagger java.time.LocalDate) => {:type "string" :format "date"}
      (rsjs/->swagger java.time.LocalTime) => {:type "string" :format "time"}))


  (fact "schema types"
    (rsjs/->swagger s/Int) => {:type "integer" :format "int64"}
    (rsjs/->swagger s/Str) => {:type "string"}
    (rsjs/->swagger s/Symbol) => {:type "string"}
    (rsjs/->swagger s/Inst) => {:type "string" :format "date-time"}
    (rsjs/->swagger s/Num) => {:type "number" :format "double"})

  (fact "containers"
    (rsjs/->swagger [Long]) => {:type "array" :items {:format "int64" :type "integer"}}
    (rsjs/->swagger #{Long}) => {:type "array" :items {:format "int64" :type "integer"} :uniqueItems true})

  (facts "nil"
    (rsjs/->swagger nil) => nil)

  (facts "unknowns"
    (fact "throw exception by default"
      (rsjs/->swagger java.util.Vector) => (throws IllegalArgumentException))
    (fact "are ignored with *ignore-missing-mappings*"
      (binding [rsjs/*ignore-missing-mappings* true]
        (rsjs/->swagger java.util.Vector)) => nil))

  (facts "models"
    (rsjs/->swagger Model) => {:$ref "#/definitions/Model"}
    (rsjs/->swagger [Model]) => {:items {:$ref "#/definitions/Model"}, :type "array"}
    (rsjs/->swagger #{Model}) => {:items {:$ref "#/definitions/Model"}, :type "array" :uniqueItems true})

  (fact "schema predicates"
    (fact "s/enum"
      (rsjs/->swagger (s/enum :kikka :kakka)) => {:type "string" :enum [:kikka :kakka]}
      (rsjs/->swagger (s/enum 1 2 3)) => {:type "integer" :format "int64" :enum (seq #{1 2 3})})

    (fact "s/maybe"
      (fact "uses wrapped value by default with x-nullable true"
        (rsjs/->swagger (s/maybe Long)) => (assoc (rsjs/->swagger Long) :x-nullable true))
      (fact "adds allowEmptyValue when for query and formData as defined by the spec"
        (rsjs/->swagger (s/maybe Long) {:in :query} :swagger) => (assoc (rsjs/->swagger Long) :allowEmptyValue true)
        (rsjs/->swagger (s/maybe Long) {:in :formData} :swagger) => (assoc (rsjs/->swagger Long) :allowEmptyValue true))
      (fact "uses wrapped value by default with x-nullable true with body"
        (rsjs/->swagger (s/maybe Long) {:in :body} :swagger) => (assoc (rsjs/->swagger Long) :x-nullable true))
      (fact "uses wrapped value for other parameters"
        (rsjs/->swagger (s/maybe Long) {:in :header} :swagger) => (rsjs/->swagger Long)
        (rsjs/->swagger (s/maybe Long) {:in :path} :swagger) => (rsjs/->swagger Long)))

    (fact "s/defrecord"
      (rsjs/->swagger User) => {:type "object",
                                :title "User",
                                :properties {:age {:type "integer", :format "int64"},
                                             :keyboard {:type "object",
                                                        :title "Keyboard",
                                                        :properties {:type {:type "string", :enum [:right :left]}},
                                                        :additionalProperties false,
                                                        :required [:type]}},
                                :additionalProperties false,
                                :required [:age :keyboard]})

    (fact "s/both -> type of the first element"
      (rsjs/->swagger (s/both Long String)) => (rsjs/->swagger Long))

    (fact "s/either -> type of the first element"
      (rsjs/->swagger (s/either Long String)) => (rsjs/->swagger Long))

    (fact "s/named -> type of schema"
      (rsjs/->swagger (s/named Long "long")) => (rsjs/->swagger Long))

    (fact "s/one -> type of schema"
      (rsjs/->swagger [(s/one Long "s")]) => (rsjs/->swagger [Long]))

    (fact "s/recursive -> type of internal schema"
      (rsjs/->swagger (s/recursive #'Model)) => (rsjs/->swagger #'Model))

    (fact "s/eq -> enum of size one"
      (rsjs/->swagger (s/eq "kikka")) => (rsjs/->swagger (s/enum "kikka")))

    (fact "s/Any"
      (rsjs/->swagger s/Any) => {}
      (rsjs/->swagger s/Any {:in :body} :swagger) => {}
      (rsjs/->swagger s/Any {:in :header} :swagger) => {:type "string"}
      (rsjs/->swagger s/Any {:in :path} :swagger) => {:type "string"}
      (rsjs/->swagger s/Any {:in :query} :swagger) => {:type "string", :allowEmptyValue true}
      (rsjs/->swagger s/Any {:in :formData} :swagger) => {:type "string", :allowEmptyValue true})

    (fact "s/conditional"
      (rsjs/->swagger (s/conditional (constantly true) Long (constantly false) String))
      => {:x-oneOf [(rsjs/->swagger Long) (rsjs/->swagger String)]}

      (fact "invalid values are removed"
        (rsjs/->swagger (s/conditional (constantly true) Long (constantly false) Currency))
        => {:x-oneOf [(rsjs/->swagger Long)]}))

    (fact "s/constrained"
      (rsjs/->swagger (s/constrained Long even?))
      => (rsjs/->swagger Long))

    (fact "s/if"
      (rsjs/->swagger (s/if (constantly true) Long String))
      => {:x-oneOf [(rsjs/->swagger Long) (rsjs/->swagger String)]})

    (fact "s/cond-pre"
      (rsjs/->swagger (s/cond-pre Model [s/Str]))
      => {:x-oneOf [(rsjs/->swagger Model) (rsjs/->swagger [s/Str])]})
    ))

(fact "Optional-key default metadata"
  (rsjs/properties {(with-meta (s/optional-key :foo) {:default "bar"}) s/Str} :swagger)
  => {:foo {:type "string" :default "bar"}}

  (fact "nil default is ignored"
    (rsjs/properties {(with-meta (s/optional-key :foo) {:default nil}) s/Str} :swagger)
    => {:foo {:type "string"}})

  (fact "pfnk schema"
    (rsjs/properties (pfnk/input-schema (p/fnk [{x :- s/Str "foo"}])) :swagger)
    => {:x {:type "string" :default "foo"}})

  (fact "pfnk schema - nil default is ignored"
    (rsjs/properties (pfnk/input-schema (p/fnk [{x :- s/Str nil}])) :swagger)
    => {:x {:type "string"}}))

(fact "Describe"
  (tabular
    (fact "Basic classes"
      (let [schema (rsjs/describe ?class ..desc.. :minimum ..val..)]
        (rsjs/json-schema-meta schema) => {:description ..desc.. :minimum ..val..}
        (rsjs/->swagger schema) => (contains {:description ..desc..})))
    ?class
    Long
    Double
    String
    Boolean
    Date
    DateTime
    LocalDate
    LocalTime
    Pattern
    UUID
    clojure.lang.Keyword
    (extension/java-time java.time.Instant)
    (extension/java-time java.time.LocalDate)
    (extension/java-time java.time.LocalTime))

  (fact "Describe Model"
    (let [schema (rsjs/describe Model ..desc..)]
      (rsjs/json-schema-meta schema) => {:description ..desc..}
      (rsjs/->swagger schema) =not=> (contains {:description ..desc..}))))

(fact "field"
  (fact "on maps"
    (let [schema (s/schema-with-name
                   (rsjs/field {(s/optional-key :name) s/Str
                                (s/optional-key :title) s/Str}
                               {:minProperties 1})
                   "schema")]

      (fact "$ref's are stripped of extra metadata"
        (rsjs/->swagger schema) => {:$ref "#/definitions/schema"})

      (fact "extra metadata is present on schema objects"
        (rsjs/schema-object schema :swagger) => (contains
                                         {:properties {:name {:type "string"}
                                                       :title {:type "string"}}
                                          :minProperties 1
                                          :additionalProperties false})))))

(fact "leiPredicate evaluation"
  (tabular
    (fact "Schema core predicates"
      (rsjs/->swagger ?pred) => (rsjs/->swagger (eval ?pred)))
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
  (fact "s/Any -values are not ignored"
    (keys (rsjs/properties {:a s/Str
                            :b s/Any} :swagger))
    => [:a :b])

  (fact "nil-values are ignored"
    (keys (rsjs/properties {:a s/Str, :b Currency} :swagger))
    => [:a])

  (fact "s/Keyword -keys are ignored"
    (keys (rsjs/properties {:a s/Str
                            s/Keyword s/Str} :swagger))
    => [:a])

  (fact "Class -keys are ignored"
    (keys (rsjs/properties {:a s/Str
                            s/Str s/Str} :swagger))
    => [:a])

  (fact "Required keyword-keys are used"
    (keys (rsjs/properties {:a s/Str
                            (s/required-key :b) s/Str} :swagger))
    => [:a :b])

  (fact "Required non-keyword-keys are NOT ignored"
    (keys (rsjs/properties {:a s/Str
                            (s/required-key "b") s/Str} :swagger))
    => [:a "b"])

  (fact "s/Any -keys are ignored"
    (keys (rsjs/properties {:a s/Str
                            s/Any s/Str} :swagger))
    => [:a])

  (fact "with unknown mappings"
    (fact "by default, exception is thrown"
      (rsjs/properties {:a String
                        :b java.util.Vector} :swagger) => (throws IllegalArgumentException))
    (fact "unknown fields are ignored ig *ignore-missing-mappings* is set"
      (binding [rsjs/*ignore-missing-mappings* true]
        (keys (rsjs/properties {:a String
                                :b java.util.Vector} :swagger)) => [:a])))

  (fact "Keeps the order of properties intact"
    (keys (rsjs/properties (linked/map :a String
                                       :b String
                                       :c String
                                       :d String
                                       :e String
                                       :f String
                                       :g String
                                       :h String) :swagger))
    => [:a :b :c :d :e :f :g :h])

  (fact "Ordered-map works with sub-schemas"
    (rsjs/properties (rsc/with-named-sub-schemas (linked/map :a String
                                                             :b {:foo String}
                                                             :c [{:bar String}] )) :swagger)
    => anything)

  (fact "referenced record-schemas"
    (s/defschema Foo (s/enum :one :two))
    (s/defschema Bar {:key Foo})

    (fact "can't get properties out of record schemas"
      (rsjs/properties Foo :swagger)) => (throws AssertionError)

    (fact "nested properties work ok"
      (keys (rsjs/properties Bar :swagger)) => [:key])))

(facts "additional-properties"
  (fact "No additional properties"
    (rsjs/additional-properties {:a s/Str} :swagger)
    => false)

  (fact "s/Keyword"
    (rsjs/additional-properties {s/Keyword s/Bool} :swagger)
    => {:type "boolean"})

  (fact "s/Any"
    (rsjs/additional-properties {s/Any s/Str} :swagger)
    => {:type "string"})

  (fact "s/Str"
    (rsjs/additional-properties {s/Str s/Bool} :swagger)
    => {:type "boolean"})

  (fact "s/Int"
    (rsjs/additional-properties {s/Int s/Str} :swagger)
    => {:type "string"}))
