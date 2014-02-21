(ns ring.swagger.schema-test
  (:require [midje.sweet :refer :all]
            [schema.core :as s]
            [cheshire.core :as cheshire]
            [clj-time.core :as t]
            [ring.swagger.schema :refer :all]
            ring.swagger.core) ;; transformers
  (:import  [java.util Date]
            [org.joda.time DateTime LocalDate]))

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
                        :m (s/both Long (s/pred odd? 'odd?))}})

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
                :m 1}})

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

(fact "models"

  (fact "model?"
    (model? AllTypes) => true
    (model? 'AllTypes) => false
    (model? #'AllTypes) => false
    (model? {:a String}) => false)

  (fact "model-var"
    (model-var AllTypes) => #'AllTypes
    (model-var 'AllTypes) => #'AllTypes
    (model-var #'AllTypes) => #'AllTypes
    (model-var {:a String}) => nil)

  (fact "model-name"
    (model-name AllTypes) => "AllTypes"
    (model-name 'AllTypes) => "AllTypes"
    (model-name #'AllTypes) => "AllTypes"
    (model-name {:a String}) => nil))

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
    (defmodel MapModel {:a String}))

  (fact "Non-map is not allowed as a model"
    (eval `(defmodel MapModel [String])) => (throws AssertionError))

  (fact "has meta-data"
    MapModel => (has-meta {:model #'MapModel}))
  (fact "model?"
    (model? MapModel) => true
    (model? {:a String}) => false))

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

(fact "coercion"
  (let [valid {:a "kikka"}
        invalid {}]

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
