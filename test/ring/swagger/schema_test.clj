(ns ring.swagger.schema-test
  (:require [midje.sweet :refer :all]
            [schema.core :as s]
            [cheshire.core :as cheshire]
            [clj-time.core :as t]
            [ring.swagger.extension :as extension]
            [ring.swagger.schema :as schema]
            ring.swagger.json)
  (:import [java.util Date UUID]
           [java.util.regex Pattern]
           [org.joda.time DateTime LocalDate LocalTime]))

(s/defschema SubType {:alive Boolean})
(s/defschema AllTypes
  {:a Boolean
   :b Double
   :c Long
   :d String
   :e {:f [s/Keyword]
       :g #{String}
       :h #{(s/enum :kikka :kakka :kukka)}
       :i Date
       :j DateTime
       :k LocalDate
       :k2 LocalTime
       :l (s/maybe String)
       :m (s/both Long (s/pred odd? 'odd?))
       :n SubType
       :o [{:p #{{:q String}}}]
       :u UUID
       :v Pattern
       :w #"a[6-9]"}})

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
                :k2 (t/time-now)
                :l nil
                :m 1
                :n {:alive true}
                :o [{:p #{{:q "abba"}}}]
                :u (UUID/fromString "77e70512-1337-dead-beef-0123456789ab")
                :v "a[6-9]"
                :w "a9"}})

;; since every java.util.regex.Pattern is not equal
(defn- pattern-to-str
  [model]
  (update-in model [:e :v] str))

(fact "All types can be read from json"
  (let [json (cheshire/generate-string model)
        jmodel (cheshire/parse-string json true)]

    (fact "json can be parsed"
      json => truthy
      jmodel => truthy)

    (fact "by default, models don't match"
      jmodel =not=> model)

    (fact "coerce! makes models match"
      (pattern-to-str (schema/coerce! AllTypes jmodel)) => model))

  (extension/java-time
    (let [types {:i java.time.Instant
                 :ld java.time.LocalDate
                 :lt java.time.LocalTime}
          model {:i (java.time.Instant/now)
                 :ld (java.time.LocalDate/now)
                 :lt (java.time.LocalTime/now)}
          json (cheshire/generate-string model)
          jmodel (cheshire/parse-string json true)]

      (fact "json can be parsed"
        json => truthy
        jmodel => truthy)

      (fact "by default, models don't match"
        jmodel =not=> model)

      (fact "coerce! makes models match"
        (schema/coerce! types jmodel) => model))))

(fact "date-time coercion"
  (fact "with millis"
    (schema/coerce! {:d Date} {:d "2014-02-24T21:37:40.477Z"}) =not=> (throws Exception))
  (fact "without millis"
    (schema/coerce! {:d Date} {:d "2014-02-24T21:37:40Z"}) =not=> (throws Exception)))

(fact "named-schema"

  (fact "schema is named"
    (s/defschema AbbaSchema {:s String})
    AbbaSchema => schema/named-schema?)

  (fact "def is not named"
    (def AbbaDef {:s String})
    AbbaDef =not=> schema/named-schema?))

(s/defschema MapModel {:a String})

(fact "coercion"
  (let [valid {:a "kikka"}
        invalid {}]

    (fact "coerce works for both models and schemas"
      (schema/coerce MapModel valid) => valid
      (schema/coerce {:a String} valid) => valid

      (schema/coerce MapModel invalid) => schema/error?
      (schema/coerce {:a String} invalid) => schema/error?)

    (fact "coerce! works for both models and schemas"
      (schema/coerce! MapModel valid) => valid
      (schema/coerce! {:a String} valid) => valid

      (schema/coerce! MapModel invalid) => (throws clojure.lang.ExceptionInfo)
      (schema/coerce! {:a String} invalid) => (throws clojure.lang.ExceptionInfo))

    (fact "both runs all predicates"
      (let [OddModel (s/both Long (s/pred odd? 'odd?))]
        (schema/coerce OddModel 1) => 1
        (schema/coerce OddModel 2) => schema/error?))))

(facts "schema coercion"
  (let [Schema {:a Long :b Double :c Boolean :d s/Keyword :u UUID}
        value {:a "1" :b "2.2" :c "true" :d "kikka" :u "77e70512-1337-dead-beef-0123456789ab"}
        target {:a 1 :b 2.2 :c true :d :kikka :u (UUID/fromString "77e70512-1337-dead-beef-0123456789ab")}]

    (fact ":query coercion can convert string to Longs, Doubles, Booleans and UUIDs"
      (schema/coerce! Schema value :query) => target)

    (fact ":json coercion cant convert string to Longs,Doubles, Booleans and UUIDs"
      (schema/coerce! Schema value :json) => (throws Exception))

    (fact "custom coersion"
      (schema/coerce! {:a String :b String}
               {:a "kikka" :b "kukka"}
               (fn [_]
                 (fn [x]
                   (if (string? x) (.toUpperCase x) x))))

      => {:a "KIKKA" :b "KUKKA"})))


