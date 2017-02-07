(ns ring.swagger.middleware-test
  (:require [midje.sweet :refer :all]
            [schema.core :as s]
            [ring.swagger.schema :as schema]
            [ring.swagger.middleware :as middleware]
            [ring.util.http-response :as http-response]))

(s/defschema P {:a Long
                :b {:c (s/enum :kikka :kakka)}})

(defn bad [_] (schema/coerce! P {:b {:c nil}}))
(defn good [_] (schema/coerce! P {:a 1, :b {:c :kikka}}))

(defn bad-async [_ respond _] (respond (schema/coerce! P {:b {:c nil}})))
(defn good-async [_ respond _] (respond (schema/coerce! P {:a 1, :b {:c :kikka}})))

(def request {})

(facts "wrap-validation-errors"

  (facts "sync"
    (fact "without middleware exception is thrown for validation error"
      (good request) =not=> (throws Exception)
      (bad request) => (throws Exception))

    (fact "with middleware exceptions are converted into bad-request"
      ((middleware/wrap-validation-errors bad) request) =>
      (http-response/bad-request {:errors {:a "missing-required-key"
                                           :b {:c "(not (#{:kikka :kakka} nil))"}}}))

    (fact "using custom :error-handler"
      ((middleware/wrap-validation-errors bad {:error-handler (constantly "FAIL")}) request) =>
      "FAIL")

    (fact "only response-exceptions are caught"
      ((middleware/wrap-validation-errors (fn [_] (throw (RuntimeException.)))) request) => (throws Exception))

    (let [failing-handler (fn [_] (s/validate {:a String} {}))]
      (fact "by default, schema.core validation errors are not caught"
        ((middleware/wrap-validation-errors failing-handler) request) => (throws Exception))
      (fact "with :catch-core-errors? false, schema.core validation errors are not caught"
        ((middleware/wrap-validation-errors failing-handler {:catch-core-errors? false}) request) => (throws Exception))
      (fact "with :catch-core-errors? true, schema.core validation errors are caught"
        ((middleware/wrap-validation-errors failing-handler {:catch-core-errors? true}) request) =>
        (http-response/bad-request {:errors {:a "missing-required-key"}}))))

  (facts "async"
    (fact "without middleware exception is thrown for validation error"
      @(good-async request (promise) (promise)) =not=> (throws Exception)
      @(bad-async request (promise) (promise)) => (throws Exception))

    (fact "with middleware exceptions are converted into bad-request"
      (let [respond (promise), raise (promise)]
        ((middleware/wrap-validation-errors bad-async) request respond raise)
        @respond => (http-response/bad-request {:errors {:a "missing-required-key"
                                                         :b {:c "(not (#{:kikka :kakka} nil))"}}})))

    (fact "using custom :error-handler"
      (let [respond (promise), raise (promise)]
        ((middleware/wrap-validation-errors bad-async {:error-handler (constantly "FAIL")}) request respond raise)
        @respond => "FAIL"))

    (fact "only response-exceptions are caught"
      (fact "raised"
        (let [respond (promise), raise (promise)]
          ((middleware/wrap-validation-errors (fn [_ _ raise] (raise (RuntimeException.)))) request respond raise)
          (class @raise) => RuntimeException))

      (fact "thrown"
        (let [respond (promise), raise (promise)]
          ((middleware/wrap-validation-errors
             (fn [_ _ raise] (throw (RuntimeException.))))
            request respond raise) => (throws RuntimeException))))

    (let [failing-handler-async (fn [_ respond _] (respond (s/validate {:a String} {})))]
      (fact "by default, schema.core validation errors are not caught"
        (let [respond (promise), raise (promise)]
          ((middleware/wrap-validation-errors
             failing-handler-async) request respond raise) => (throws Exception)))
      (fact "with :catch-core-errors? false, schema.core validation errors are not caught"
        (let [respond (promise), raise (promise)]
          ((middleware/wrap-validation-errors
             failing-handler-async {:catch-core-errors? false}) request respond raise) => (throws Exception)))
      (fact "with :catch-core-errors? true, schema.core validation errors are caught"
        (let [respond (promise), raise (promise)]
          ((middleware/wrap-validation-errors failing-handler-async {:catch-core-errors? true}) request respond raise)
          @respond => (http-response/bad-request {:errors {:a "missing-required-key"}}))))))

(fact "stringify-error"
  (middleware/stringify-error (s/check P {:b {:bad 1}})) => {:a "missing-required-key"
                                                             :b {:bad "disallowed-key"
                                                                 :c "missing-required-key"}})

(fact "comp-mw"
  (let [mw1 (fn [_ & params] (fn [_] (apply hash-map params)))
        mw2 (middleware/comp-mw mw1 :abba 2)
        mw3 (middleware/comp-mw mw2 :abba 3 :jabba 3)]
    ((mw1 identity) request) => {}
    ((mw1 identity :abba 1) request) => {:abba 1}
    ((mw2 identity) request) => {:abba 2}
    ((mw3 identity) request) => {:abba 3 :jabba 3}
    ((mw3 identity :abba 4 :jabba 4 :doo 4) request) => {:abba 4 :jabba 4 :doo 4}))

(fact "setting and getting swagger-data by middlewares"
  (let [request {:uri ..uri.., :request-method ..method..}]

    (fact "by default, no swagger-data is attached"
      (middleware/get-swagger-data request) => nil)

    (fact "middlwares can attach swagger-data to request"
      (let [enchanced
            (-> request
                (middleware/set-swagger-data
                  {:produces [:json :edn]})
                (middleware/set-swagger-data
                  assoc :consumes [:json :edn]))]

        (fact "original request is preserved"
          enchanced => (contains request))

        (fact "swagger-data can be extracted from request"
          (middleware/get-swagger-data enchanced)
          => {:produces [:json :edn]
              :consumes [:json :edn]})))

    (fact "wrap-swagger-data"
      (let [enchanced
            ((middleware/wrap-swagger-data identity {:produces [:json :edn]
                                                     :consumes [:json :edn]}) request)]

        (fact "original request is preserved"
          enchanced => (contains request))

        (fact "swagger-data can be extracted from request"
          (middleware/get-swagger-data enchanced)
          => {:produces [:json :edn]
              :consumes [:json :edn]})))))
