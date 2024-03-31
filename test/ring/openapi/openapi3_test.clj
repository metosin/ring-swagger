(ns ring.openapi.openapi3-test
  (:require [clojure.test :refer :all]
            [ring.openapi.openapi3 :refer :all]
            [schema.core :as s]
            [ring.openapi.openapi3-schema :as full-schema]
            [ring.swagger.json-schema :as rsjs]
            [ring.swagger.extension :as extension]
            [ring.openapi.validator :as validator]
            [linked.core :as linked]
            [ring.util.http-status :as status]
            [midje.sweet :refer :all])
  (:import (java.time Instant)
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
   (validator/validate (openapi-json swagger options))))

(defn validate [swagger & [options]]
  (s/with-fn-validation
   (if-let [input-errors (s/check OpenApi swagger)]
     {:input-errors input-errors}
     (if-let [output-errors (validate-swagger-json swagger options)]
       {:output-errors output-errors}))))

(def a-complete-swagger
  {:swagger "3.0.0"
   :info {:version "version"
          :title "title"
          :description "description"
          :termsOfService "jeah"
          :contact {:name "name"
                    :url "http://someurl.com"
                    :email "tommi@example.com"}
          :license {:name "name"
                    :url "http://someurl.com"}}
   :servers [{:url "somehost:8080"}]
   :externalDocs {:url "http://someurl.com"
                  :description "more info"}
   :tags [{:name "pet",
           :description "Everything about your Pets",
           :externalDocs {:description "Find out more", :url "http://swagger.io"}}
          {:name "store",
           :description "Access to Petstore orders"}
          {:name "user",
           :description "Operations about user",
           :externalDocs {:description "Find out more about our store", :url "http://swagger.io"}}]
   :paths {"/api/:id" {:get {:tags ["pet"]
                             :summary "summary"
                             :description "description"
                             :operationId "operationId"
                             :parameters {:query  (merge Anything {:x Long :y Long})
                                          :path   {:id String}
                                          :header Anything}
                             :responses {200 {:description "ok"
                                              :content     {"application/json" {:schema nil}}}
                                         400 {:description "not found"
                                              :content     {"application/json" {:schema NotFound}}}}}}
           "/api/parrots" {:get {:responses {200 {:content     {"application/json" {:schema Parrot}}
                                                  :description ""}}}}}})
;;
;; facts
;;

(fact "empty spec"
      (let [swagger {}]
        (validate swagger) => nil))

(fact "minimalistic spec"
      (let [swagger {:paths {"/ping" {:get {}}}}]
        (validate swagger) => nil))

#_(fact "more complete spec"
        (validate a-complete-swagger) => nil)

(extension/java-time
 (fact "spec with java.time"
       (let [model   {:i  Instant
                      :ld java.time.LocalDate
                      :lt java.time.LocalTime}
             swagger {:paths {"/time" {:post {:parameters {:query model}}}}}]

         (validate swagger) => nil)))