(ns ring.swagger.schema-test
  (:require [midje.sweet :refer :all]
            [schema.core :as s]
            [cheshire.core :as cheshire]
            [clj-time.core :as t]
            [ring.swagger.schema :refer :all]
            [ring.swagger.common :refer :all]
            ring.swagger.core) ;; transformers
  (:import  [java.util Date]
            [org.joda.time DateTime LocalDate]))

(defmodel SubType  {:alive Boolean})
(defmodel AllTypes {:a Boolean
                    :b Double
                    :c Long
                    :d String
                    :e {:f [Keyword]
                        :g #{String}
                        :h #{(s/enum :kikka :kakka :kukka)}
                        :i Date
                        :j DateTime
                        :k LocalDate
                        :l (s/maybe String)
                        :m (s/both Long (s/pred odd? 'odd?))
                        :n SubType
                        :o [{:p #{{:q String}}}]}})

(def model {:a true
            :b 2.2
            :c 16
            :d "kikka"
            :e {:f [:kikka :kikka :kukka]
                :g #{"kikka" "kakka"}
                :h #{:kikka}
                :i (Date.)
                :j (t/now)
                :k (t/today)
                :l nil
                :m 1
                :n {:alive true}
                :o [{:p #{{:q "abba"}}}]}})

(fact "All types can be read from json"
  (let [json   (cheshire/generate-string model)
        jmodel (cheshire/parse-string json true)]

    (fact "json can be parsed"
      json => truthy
      jmodel => truthy)

    (fact "by default, models don't match"
      jmodel =not=> model)

    (fact "coerce! makes models match"
      (coerce! AllTypes jmodel) => model)))

(fact "date-time coercion"
  (fact "with millis"
    (coerce! {:d Date} {:d "2014-02-24T21:37:40.477Z"}) =not=> (throws Exception))
  (fact "without millis"
    (coerce! {:d Date} {:d "2014-02-24T21:37:40Z"}) =not=> (throws Exception)))

(facts "types"

  (fact "basic types can act as fields"
    (doseq [type (vals type-map)]
      (fact {:midje/description (s/explain type)}
        (meta (field type {:a 1})) => {:a 1})))

  (fact "derived types can act as fields"
    (doseq [type (keys type-map)]
      (fact {:midje/description (s/explain type)}
        (meta (field type {:a 1})) => {:a 1}))))

(defn has-meta [expected] (chatty-checker [x] (= (meta x) expected)))

(fact "defmodel"

  (fact "Map is allowed as a model"
    (defmodel MapModel {:x String
                        :y String}))

  (fact "Something that evaluates as a map is allowed as a model"
    (defmodel PimpedMapModel (dissoc MapModel :y)))

  (fact "Non-map is not allowed as a model"
    (eval '(defmodel MapModel [String])) => (throws AssertionError))

  (fact "has meta-data"
    MapModel => (has-meta {:name 'MapModel}))
  (fact "model?"
    (s/schema-name MapModel) => 'MapModel
    (s/schema-name {:a String}) => falsey))

(fact "field"

  (fact "field set meta-data to it"
    (field String {:kikka :kakka}) => (has-meta {:kikka :kakka}))

  (doseq [c (keys type-map)]
    (fact {:midje/description (str "can't set meta-data to " c)}
      (with-meta c {:a 1}) => (throws Exception)))

  (doseq [c (vals type-map)]
    (fact {:midje/description (str "can set meta-data to " (s/explain c))}
      (with-meta c {:a 1}) =not=> (throws Exception)))

  (fact "field set meta-data to it"
    (field String {:kikka :kakka}) => (has-meta {:kikka :kakka})))

(fact "named-schema"

  (fact "schema is named"
    (s/defschema AbbaSchema {:s String})
    AbbaSchema => named-schema?)

  (fact "model is named"
    (defmodel AbbaModel {:s String})
    AbbaModel => named-schema?)

  (fact "def is not named"
    (def AbbaDef {:s String})
    AbbaDef =not=> named-schema?))

(fact "coercion"
  (let [valid {:a "kikka"}
        invalid {}]

    (defmodel MapModel {:a String})

    (fact "coerce works for both models and schemas"
      (coerce MapModel valid) => valid
      (coerce {:a String} valid) => valid

      (coerce MapModel invalid) => error?
      (coerce {:a String} invalid) => error?)

    (fact "coerce! works for both models and schemas"
      (coerce! MapModel valid) => valid
      (coerce! {:a String} valid) => valid

      (coerce! MapModel invalid) => (throws clojure.lang.ExceptionInfo)
      (coerce! {:a String} invalid) => (throws clojure.lang.ExceptionInfo))

    (fact "both runs all predicates"
      (let [OddModel (s/both Long (s/pred odd? 'odd?))]
        (coerce OddModel 1) => 1
        (coerce OddModel 2) => error?))))

(defmodel Customer {:id Long
                    :address {:street String
                              (s/optional-key :country) {:code #{(s/enum :fi :sv)}
                                                         :name String}}})

(facts "nested models"
  (fact ".. have generated sub-models and are referenced from parent"
    (s/schema-name (get-in Customer [:address])) => 'CustomerAddress
    CustomerAddress => {:street String
                        (s/optional-key :country) {:code #{(s/enum :fi :sv)}
                                                   :name String}})

  (fact ".. for deeper level also"
    (s/schema-name (get-in Customer [:address (s/optional-key :country)])) => 'CustomerAddressCountry
    CustomerAddressCountry => {:code #{(s/enum :fi :sv)}
                               :name String}))

(facts "parameter coercion"

  (let [Model {:a Long :b Double :c Boolean :d Keyword}
        query {:a "1"  :b "2.2"  :c "true"  :d "kikka"}
        value {:a 1    :b 2.2    :c true    :d :kikka}]

    (fact "query-coercion can convert string to Longs, Doubles and Booleans"
      (coerce! Model query :query) => value)

    (fact "json-coercion cant convert string to Longs,Doubles and Booleans"
      (coerce! Model query :json) => (throws Exception))

    #_(fact "both-coercion can also convert string to Longs, Doubles and Booleans"
       (coerce! Model query :both) => value)))
