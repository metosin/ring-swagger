(ns ring.swagger.middleware-test
  (:require [midje.sweet :refer :all]
            [schema.core :as s]
            [ring.swagger.schema :refer :all]
            [ring.swagger.middleware :refer :all]
            [ring.util.http-response :refer :all]))

(defmodel P {:a {:b {:c (s/enum :kikka :kakka)
                     :d Long}}})

(defmacro always [& body] `(fn [_#] ~@body))

(def bad (always (coerce! P {:a {:b {:c nil}}})))
(def good (always (coerce! P {:a {:b {:c :kikka
                                      :d 1}}})))

(fact "catch-response"

  (facts "without middleware exception is thrown for validation error"
    (good ..request..) =not=> (throws Exception)
    (bad ..request..) => (throws Exception))

  (facts "with middleware exceptions are converted into bad-request"
    ((catch-validation-errors bad) ..request..) => (bad-request {:errors {:a {:b {:c "(not (#{:kikka :kakka} nil))"
                                                                                  :d "missing-required-key"}}}}))

  (fact "only response-exceptions are caught"
    ((catch-validation-errors (always (throw (RuntimeException.)))) ..request..) => (throws Exception)))
