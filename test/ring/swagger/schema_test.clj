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
                        :k LocalDate}})

(def model {:a true
            :b 2.2
            :c 16
            :d "kikka"
            :e {:f [:kikka :kikka :kukka]
                :g #{"kikka" "kakka"}
                :h #{:kikka}
                :i (Date.)
                :j (t/now)
                :k (t/today)}})

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
    (model? {:int s/Int}) => false)

  (fact "model-of"
    (model-of AllTypes) => #'AllTypes
    (model-of 'AllTypes) => #'AllTypes
    (model-of #'AllTypes) => #'AllTypes)

  (fact "schema-name"
    (schema-name AllTypes) => "AllTypes"
    (schema-name 'AllTypes) => "AllTypes"
    (schema-name #'AllTypes) => "AllTypes"))

(facts "types"

  (fact "basic types can act as fields"
    (doseq [type (vals type-map)]
      (fact {:midje/description (s/explain type)}
        (meta (field type {:a 1})) => {:a 1})))

  (fact "derived types can act as fields"
    (doseq [type (keys type-map)]
      (fact {:midje/description (s/explain type)}
        (meta (field type {:a 1})) => {:a 1}))))
