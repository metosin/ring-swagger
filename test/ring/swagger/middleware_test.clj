(ns ring.swagger.middleware-test
  (:require [midje.sweet :refer :all]
            [schema.core :as s]
            [ring.swagger.schema :refer :all]
            [ring.swagger.middleware :refer :all]
            [ring.util.http-response :refer :all]))

(s/defschema P {:a Long
                :b {:c (s/enum :kikka :kakka)}})

(defn bad  [_] (coerce! P {:b {:c nil}}))
(defn good [_] (coerce! P {:a 1
                           :b {:c :kikka}}))
(defn fail [_] (throw (RuntimeException.)))

(fact "catch-response"

  (facts "without middleware exception is thrown for validation error"
    (good ..request..) =not=> (throws Exception)
    (bad  ..request..)   =>   (throws Exception))

  (facts "with middleware exceptions are converted into bad-request"
    ((wrap-validation-errors bad) ..request..) => (bad-request {:errors {:a "missing-required-key"
                                                                         :b {:c "(not (#{:kikka :kakka} nil))"}}}))

  (fact "only response-exceptions are caught"
    ((wrap-validation-errors (fail ..request..))) => (throws Exception)))

(fact "stringify-error"
  (stringify-error (s/check P {:b {:bad 1}})) => {:a "missing-required-key"
                                                  :b {:bad "disallowed-key"
                                                      :c "missing-required-key"}})
