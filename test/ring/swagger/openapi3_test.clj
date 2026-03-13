(ns ring.swagger.openapi3-test
  (:require [clojure.test :refer :all]
            [ring.swagger.openapi3 :refer :all]
            [schema.core :as s]
            [ring.swagger.openapi3-validator :as validator]
            [midje.sweet :refer :all])
  (:import
           [java.util Date UUID]
           [java.util.regex Pattern]
           [org.joda.time DateTime LocalDate LocalTime]))

(s/defschema Anything {s/Keyword s/Any})
(s/defschema Nothing {})

(s/defschema LegOfPet {:length Long})

(s/defschema Pet {:id Long
                  :name String
                  :leg LegOfPet
                  (s/optional-key :weight) Double})

(s/defschema Parrot {:name String
                     :type {:name String}})

(s/defschema Turtle {:name String
                     :tags  (s/if map? {s/Keyword s/Keyword} [String])})

(s/defschema NotFound {:message s/Str})

(defn validate-swagger-json [swagger & [options]]
  (s/with-fn-validation
    (let [response (openapi-json swagger options)]
      (validator/validate response))))

(defn validate [swagger & [options]]
  (s/with-fn-validation
   (if-let [input-errors (s/check OpenApi swagger)]
     {:input-errors input-errors}
     (if-let [output-errors (validate-swagger-json swagger options)]
       {:output-errors output-errors}))))

(def a-complete-swagger-2
  {:info         {:version        "version"
                  :title          "title"
                  :description    "description"
                  :termsOfService "jeah"
                  :contact        {:name  "name"
                                   :url   "http://someurl.com"
                                   :email "tommi@example.com"}
                  :license        {:name "name"
                                   :url  "http://someurl.com"}}
   :servers      [{:url "somehost:8080"}]
   :externalDocs {:url         "http://someurl.com"
                  :description "more info"}
   :tags         [{:name         "pet",
                   :description  "Everything about your Pets",
                   :externalDocs {:description "Find out more", :url "http://swagger.io"}}
                  {:name        "store",
                   :description "Access to Petstore orders"}
                  {:name         "user",
                   :description  "Operations about user",
                   :externalDocs {:description "Find out more about our store", :url "http://swagger.io"}}]
   :paths        {"/api/:id"       {:get {:tags         ["pet"]
                                          :summary      "summary"
                                          :description  "description"
                                          :externalDocs {:url         "http://someurl.com"
                                                         :description "more info"}
                                          :operationId  "operationId"
                                          :parameters   {:query  (merge Anything {:x Long :y Long})
                                                         :path   {:id String}
                                                         :header Anything}
                                          :responses    {"200"     {:description "ok"
                                                                    :content     {"application/json" {:schema nil}}}
                                                         "400"     {:description "not found"
                                                                    :content     {"application/json" {:schema NotFound}}}
                                                         "default" {:description "error"
                                                                    :content     {"application/json" {:schema {:code Long}}}}}}}
                  "/api/parrots"   {:get {:responses {"200" {:description "ok"
                                                             :content     {"application/json" {:schema Parrot}}}}}}
                  "/api/all-types" {:get {:requestBody {:content {"application/json" {:a Boolean
                                                                                      :b Double
                                                                                      :c Long
                                                                                      :d String
                                                                                      :e {:f  [s/Keyword]
                                                                                          :g  #{String}
                                                                                          :h  #{(s/enum :kikka :kakka :kukka)}
                                                                                          :i  Date
                                                                                          :j  DateTime
                                                                                          :k  LocalDate
                                                                                          :k2 LocalTime
                                                                                          :l  (s/maybe String)
                                                                                          :m  (s/both Long (s/pred odd? 'odd?))
                                                                                          :o  [{:p #{{:q String}}}]
                                                                                          :u  UUID
                                                                                          :v  Pattern
                                                                                          :w  #"a[6-9]"}}}}
                                          :responses   {"200" {:description "file"
                                                               :content     {"application/json" {:schema Parrot}}}}}}
                  "/api/pets"      {:get  {:parameters  {:query (merge Anything {:x Long :y Long})}
                                           :requestBody {:content {"application/json" Pet}}
                                           :responses   {"200"     {:description "ok"
                                                                    :content     {"application/json" {:schema {:sum Long}}}}
                                                         "default" {:description "error"
                                                                    :content     {"application/json" {:schema Parrot}}}}}
                                    :post {:parameters  {:query (merge Anything {:x Long :y Long})}
                                           :requestBody {:content {"application/json" [Pet]}}
                                           :responses   {"200"     {:content {"application/xml" {:schema {:sum Long}}}}
                                                         "default" {:content {"application/json" {:schema {:code Long}}}
                                                                    :headers {:location String}}}}
                                    :put  {:parameters  {:query {:x (s/maybe String)}}
                                           :requestBody {:content {"application/json" [(s/maybe Pet)]}}
                                           :responses   {"200" {:description "ok"
                                                                :content     {"application/json" {:schema {:sum (s/maybe Long)}}}}}}}
                  "/api/turtle"    {:get  {:parameters  {:query (merge Anything {:x Long :y Long})}
                                           :requestBody {:content {"application/json" Turtle}}
                                           :responses   {"200"     {:description "ok"
                                                                    :content     {"application/json" {:schema {:sum Long}}}}
                                                         "default" {:description "error"
                                                                    :content     {"application/json" {:schema {:code Long}}}}}}
                                    :post {:parameters  {:query (merge Anything {:x Long :y Long})}
                                           :requestBody {:content {"application/json" [Turtle]}}
                                           :responses   {"200"     {:content {"application/json" {:schema {:sum Long}}}}
                                                         "default" {:content {"application/json" {:schema {:code Long}}}
                                                                    :headers (merge Anything {:location String})}}}
                                    :put  {:parameters  {:query {:x (s/maybe String)}}
                                           :requestBody {:content {"application/json" [(s/maybe Turtle)]}}
                                           :responses   {"200" {:description "ok"
                                                                :content     {"application/json" {:schema {:sum (s/maybe Long)}}}}}}}}})

;;
;; facts
;;
(fact "more complete spec"
      (validate a-complete-swagger-2) => nil)
