(ns ring.swagger.swagger2-test
  (:require [schema.core :as s]
            [ring.swagger.swagger2 :refer :all]
            [ring.swagger.validator :as validator]
            [cheshire.core :as json]
            [midje.sweet :refer :all])
  (:import  [java.util Date UUID]
            [java.util.regex Pattern]
            [org.joda.time DateTime LocalDate]))


(s/defschema LegOfPet {:length Long})

(s/defschema Pet {:id Long
                  :name String
                  :leg LegOfPet
                  (s/optional-key :weight) Double})

(s/defschema Parrot {:name String
                     :type {:name String}})

(s/defschema NotFound {:message s/Str})

(defn validate-swagger-json [swagger & [options]]
  (validator/validate (json/generate-string (swagger-json swagger options))))

(defn validate [swagger & [options]]
  (if-let [input-errors (s/check Swagger swagger)]
    {:input-errors input-errors}
    (if-let [output-errors (validate-swagger-json swagger options)]
      {:output-errors output-errors})))

;;
;; facts
;;

(fact "empty spec"
  (let [swagger {}]
    (validate swagger) => nil))

(fact "minimalistic spec"
  (let [swagger {:paths {"/ping" {:get {}}}}]
    (validate swagger) => nil))

(fact "more complete spec"
  (let [swagger {:swagger  "2.0"
                 :info     {:version        "version"
                            :title          "title"
                            :description    "description"
                            :termsOfService "jeah"
                            :contact        {:name  "name"
                                             :url   "url"
                                             :email "tommi@example.com"}
                            :license        {:name "name"
                                             :url  "url"}
                            :x-kikka        "jeah"}
                 :basePath "/"
                 :consumes ["application/json" "application/edn"]
                 :produces ["application/json" "application/edn"]
                 :paths    {"/api/:id"     {:get {:tags         [:tag1 :tag2 :tag3]
                                                  :summary      "summary"
                                                  :description  "description"
                                                  :externalDocs {:url         "url"
                                                                 :description "more info"}
                                                  :operationId  "operationId"
                                                  :consumes     ["application/xyz"]
                                                  :produces     ["application/xyz"]
                                                  :parameters   {:body     Nothing
                                                                 :query    (merge Anything {:x Long :y Long})
                                                                 :path     {:id String}
                                                                 :header   Anything
                                                                 :formData Anything}
                                                  :responses    {200      {:description "ok"
                                                                           :schema      {:sum Long}}
                                                                 400      {:description "not found"
                                                                           :schema      NotFound}
                                                                 :default {:description "error"
                                                                           :schema      {:code Long}}}}}
                            "/api/parrots" {:get {:responses {200 {:schema      Parrot
                                                                   :description ""}}}}
                            "/api/all-types" {:get {:parameters {:body {:a Boolean
                                                                        :b Double
                                                                        :c Long
                                                                        :d String
                                                                        :e {:f [s/Keyword]
                                                                            :g #{String}
                                                                            :h #{(s/enum :kikka :kakka :kukka)}
                                                                            :i Date
                                                                            :j DateTime
                                                                            :k LocalDate
                                                                            :l (s/maybe String)
                                                                            :m (s/both Long (s/pred odd? 'odd?))
                                                                            :o [{:p #{{:q String}}}]
                                                                            :u UUID
                                                                            :v Pattern
                                                                            :w #"a[6-9]"}}}}}
                            "/api/pets"    {:get  {:parameters {:body     Pet
                                                                :query    (merge Anything {:x Long :y Long})
                                                                :path     Nothing
                                                                :header   Anything
                                                                :formData Anything}
                                                   :responses  {200      {:description "ok"
                                                                          :schema      {:sum Long}}
                                                                :default {:description "error"
                                                                          :schema      {:code Long}}}}
                                            :post {:parameters {:body     #{Pet}
                                                                :query    (merge Anything {:x Long :y Long})
                                                                :path     Nothing
                                                                :header   Anything
                                                                :formData Anything}
                                                   :responses  {200      {:schema      {:sum Long}}
                                                                :default {:schema      {:code Long}
                                                                          :headers     {:location String}}}}
                                            :put  {:parameters {:body     [Pet]
                                                                :query    (merge Anything {:x Long :y Long})
                                                                :path     Nothing
                                                                :header   Anything
                                                                :formData Anything}
                                                   :responses  {200      {:description "ok"
                                                                          :schema      {:sum Long}}
                                                                :default {:description "error"}}}}}}]

    (validate swagger) => nil))

(defrecord InvalidElement [])

(facts "with missing schema -> json schema mappings"

  (fact "non-body-parameters"
    (let [swagger {:paths {"/hello" {:get {:parameters {:query {:name (->InvalidElement)}}}}}}]

      (fact "dy default, exception is throws when generating json schema"
        (validate swagger) => (throws IllegalArgumentException))

      (fact "with :ignore-missing-mappings errors (and mappings) are ignored"
        (validate swagger {:ignore-missing-mappings? true}) => nil)))

  (fact "body-parameters"
    (let [swagger {:paths {"/hello" {:post {:parameters {:body {:name (->InvalidElement)
                                                                :age  s/Num}}}}}}]
      (fact "dy default, exception is throws when generating json schema"
        (validate swagger) => (throws IllegalArgumentException))

      (fact "with :ignore-missing-mappings errors (and mappings) are ignored"
        (validate swagger {:ignore-missing-mappings? true}) => nil))))

(facts "empty-responses-messages?"
  (let [swagger {:paths {"/hello" {:post {:responses {200 {}
                                                      425 {}}}}}}]
    (validate swagger) => nil

    (swagger-json swagger)
    => (contains {:paths
                  {"/hello"
                   {:post
                    {:responses
                     {200 {:description ""}
                      425 {:description ""}}}}}})

    (swagger-json swagger {:http-response-messages? true})
    => (contains {:paths
                  {"/hello"
                   {:post
                    {:responses
                     {200 {:description "OK"}
                      425 {:description "The collection is unordered."}}}}}})))
