(ns ring.swagger.swagger2-test
  (:require [schema.core :as s]
            [ring.swagger.swagger2 :refer :all]
            [ring.swagger.validator :as validator]
            [cheshire.core :as json]
            [ring.util.http-status :as status]
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
  (let [swagger {:paths {"/ping" {:get nil}}}]
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

(facts "handling empty responses"
  (let [swagger {:paths {"/hello" {:post {:responses {200 nil
                                                      425 nil
                                                      500 {:description "FAIL"}}}}}}]

    (fact "with defaults"
      (validate swagger) => nil

      (swagger-json swagger)
      => (contains {:paths
                    {"/hello"
                     {:post
                      {:responses
                       {200 {:description ""}
                        425 {:description ""}
                        500 {:description "FAIL"}}}}}}))

    (fact ":default-response-description-fn option overriden"
      (let [options {:default-response-description-fn #(get-in status/status [% :description])}]
        (validate swagger options) => nil

        (swagger-json swagger options)
        => (contains {:paths
                      {"/hello"
                       {:post
                        {:responses
                         {200 {:description "OK"}
                          425 {:description "The collection is unordered."}
                          500 {:description "FAIL"}}}}}})))))

(s/defschema ResponseModel {:id s/Str})

(defn schema-name [m]
  (-> m first val (subs (.length "#/definitions/"))))

(def valid-reference (just {:$ref anything}))
(defn contains-model-definition [model-name] (contains {model-name anything}))

(defn extract-body-schema [swagger-spec path f]
  (let [operation (get-in swagger-spec [:paths path :post])
        definitions (:definitions swagger-spec)
        schema (get-in operation [:parameters 0 :schema])
        name (schema-name (f schema))]
    {:name name
     :schema schema
     :definitions definitions
     :defined? (boolean (definitions name))}))

(defn extract-response-schema [swagger-spec code f]
  (let [operation (get-in swagger-spec [:paths "/responses" :post])
        definitions (:definitions swagger-spec)
        schema (get-in operation [:responses code :schema])
        name (schema-name (f schema))]
    {:name name
     :schema schema
     :definitions definitions
     :defined? (boolean (definitions name))}))

(facts "transforming subschemas"
  (let [model {:id s/Str}
        swagger {:paths {"/responses" {:post {:responses {200 {:schema ResponseModel}
                                                          201 {:schema [model]}
                                                          202 {:schema #{model}}
                                                          203 {:schema (s/maybe model)}
                                                          204 {:schema [(s/maybe model)]}
                                                          205 {:schema #{(s/maybe model)}}}}}
                         "/body1" {:post {:parameters {:body model}}}
                         "/body2" {:post {:parameters {:body [model]}}}
                         "/body3" {:post {:parameters {:body #{model}}}}
                         "/body4" {:post {:parameters {:body (s/maybe model)}}}
                         "/body5" {:post {:parameters {:body [(s/maybe model)]}}}
                         "/body6" {:post {:parameters {:body #{(s/maybe model)}}}}}}
        spec (swagger-json swagger)]

    (validate swagger) => nil

    (facts "body schemas"

      (fact "anonymous body schema"
        (let [{:keys [name schema defined?]} (extract-body-schema spec "/body1" identity)]
          schema => (just {:$ref anything})
          name => #"Body.*"
          defined? => true))

      (fact "body schema in vector"
        (let [{:keys [name schema defined?]} (extract-body-schema spec "/body2" :items)]
          schema => (just {:items valid-reference
                           :type  "array"})
          name => #"Body.*"
          defined? => true))

      (fact "body schema in set"
        (let [{:keys [name schema defined?]} (extract-body-schema spec "/body3" :items)]
          schema => (just {:items       valid-reference
                           :uniqueItems true
                           :type        "array"})
          name => #"Body.*"
          defined? => true))

      (fact "body schema in predicate"
        (let [{:keys [name schema defined?]} (extract-body-schema spec "/body4" identity)]
          schema => valid-reference
          name => #"Body.*"
          defined? => true))

      (fact "body schema in predicate in vectors"
        (let [{:keys [name schema defined?]} (extract-body-schema spec "/body5" :items)]
          schema => (just {:items       valid-reference
                           :type        "array"})
          name => #"Body.*"
          defined? => true))

      (fact "body schema in predicate in sets"
        (let [{:keys [name schema defined?]} (extract-body-schema spec "/body6" :items)]
          schema => (just {:items       valid-reference
                           :uniqueItems true
                           :type        "array"})
          name => #"Body.*"
          defined? => true)))

    (facts "response schemas"
      (let [operation (get-in spec [:paths "/responses" :post])
            definitions (:definitions spec)]

        (fact "named response models"
          (let [{:keys [name schema defined?]} (extract-response-schema spec 200 identity)]
            schema => valid-reference
            name => "ResponseModel"
            defined? => true))

        (fact "response models in vectors"
          (let [{:keys [name schema defined?]} (extract-response-schema spec 201 :items)]
            schema => (just {:items valid-reference
                             :type  "array"})
            name => #"Response.*"
            defined? => true))

        (fact "response models in sets"
          (let [{:keys [name schema defined?]} (extract-response-schema spec 202 :items)]
            schema => (just {:items       valid-reference
                             :uniqueItems true
                             :type        "array"})
            name => #"Response.*"
            defined? => true))

        (fact "response models in predicates"
          (let [{:keys [name schema defined?]} (extract-response-schema spec 203 identity)]
            schema => valid-reference
            name => #"Response.*"
            defined? => true))

        (fact "response models in predicates in vectors"
          (let [{:keys [name schema defined?]} (extract-response-schema spec 204 :items)]
            schema => (just {:items valid-reference
                             :type  "array"})
            name => #"Response.*"
            defined? => true))

        (fact "response models in predicates in sets"
          (let [{:keys [name schema defined?]} (extract-response-schema spec 205 :items)]
            schema => (just {:items       valid-reference
                             :uniqueItems true
                             :type        "array"})
            name => #"Response.*"
            defined? => true))))))
