(ns ring.swagger.middleware-test
  (:require [midje.sweet :refer :all]
            [schema.core :as s]
            [ring.swagger.schema :refer :all]
            [ring.swagger.middleware :refer :all]
            [ring.util.http-response :refer :all]))

(s/defschema P {:a Long
                :b {:c (s/enum :kikka :kakka)}})

(defn bad  [_] (coerce! P {:b {:c nil}}))
(defn good [_] (coerce! P {:a 1, :b {:c :kikka}}))

(facts "catch-response"

  (fact "without middleware exception is thrown for validation error"
    (good ..request..) =not=> (throws Exception)
    (bad  ..request..)   =>   (throws Exception))

  (fact "with middleware exceptions are converted into bad-request"
    ((wrap-validation-errors bad) ..request..) =>
    (bad-request {:errors {:a "missing-required-key"
                           :b {:c "(not (#{:kikka :kakka} nil))"}}}))

  (fact "using custom :error-handler"
    ((wrap-validation-errors bad :error-handler (constantly "FAIL")) ..request..) =>
    "FAIL")

  (fact "only response-exceptions are caught"
    ((wrap-validation-errors (fn [_] (throw (RuntimeException.)))) ..request..) => (throws Exception))

  (let [failing-handler (fn [_] (s/validate {:a String} {}))]
    (fact "by default, schema.core validation errors are not caught"
      ((wrap-validation-errors failing-handler)) => (throws Exception))
    (fact "with :catch-core-errors? false, schema.core validation errors are not caught"
      ((wrap-validation-errors failing-handler :catch-core-errors? false)) => (throws Exception))
    (fact "with :catch-core-errors? truem, schema.core validation errors are caught"
      ((wrap-validation-errors failing-handler :catch-core-errors? true) ..request..) =>
      (bad-request {:errors {:a "missing-required-key"}}))))

(fact "stringify-error"
  (stringify-error (s/check P {:b {:bad 1}})) => {:a "missing-required-key"
                                                  :b {:bad "disallowed-key"
                                                      :c "missing-required-key"}})
