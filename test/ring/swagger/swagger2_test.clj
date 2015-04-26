(ns ring.swagger.swagger2-test
  (:require [schema.core :as s]
            [ring.swagger.swagger2 :refer :all]
            [ring.swagger.validator :as v]
            [ring.util.http-status :as status]
            [midje.sweet :refer :all])
  (:import [java.util Date UUID]
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
  (s/with-fn-validation
    (v/validate (swagger-json swagger options))))

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
  (let [swagger {:swagger "2.0"
                 :info {:version "version"
                        :title "title"
                        :description "description"
                        :termsOfService "jeah"
                        :contact {:name "name"
                                  :url "url"
                                  :email "tommi@example.com"}
                        :license {:name "name"
                                  :url "url"}
                        :x-kikka "jeah"}
                 :basePath "/"
                 :consumes ["application/json" "application/edn"]
                 :produces ["application/json" "application/edn"]
                 :paths {"/api/:id" {:get {:tags [:tag1 :tag2 :tag3]
                                           :summary "summary"
                                           :description "description"
                                           :externalDocs {:url "url"
                                                          :description "more info"}
                                           :operationId "operationId"
                                           :consumes ["application/xyz"]
                                           :produces ["application/xyz"]
                                           :parameters {:body nil
                                                        :query (merge Anything {:x Long :y Long})
                                                        :path {:id String}
                                                        :header Anything
                                                        :formData Anything}
                                           :responses {200 {:description "ok"
                                                            :schema nil}
                                                       400 {:description "not found"
                                                            :schema NotFound}
                                                       :default {:description "error"
                                                                 :schema {:code Long}}}}}
                         "/api/parrots" {:get {:responses {200 {:schema Parrot
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
                         "/api/pets" {:get {:parameters {:body Pet
                                                         :query (merge Anything {:x Long :y Long})
                                                         :path Nothing
                                                         :header Anything
                                                         :formData Anything}
                                            :responses {200 {:description "ok"
                                                             :schema {:sum Long}}
                                                        :default {:description "error"
                                                                  :schema {:code Long}}}}
                                      :post {:parameters {:body #{Pet}
                                                          :query (merge Anything {:x Long :y Long})
                                                          :path Nothing
                                                          :header Anything
                                                          :formData Anything}
                                             :responses {200 {:schema {:sum Long}}
                                                         :default {:schema {:code Long}
                                                                   :headers {:location String}}}}
                                      :put {:parameters {:body [Pet]
                                                         :query (merge Anything {:x Long :y Long})
                                                         :path Nothing
                                                         :header Anything
                                                         :formData Anything}
                                            :responses {200 {:description "ok"
                                                             :schema {:sum Long}}
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
                                                                :age s/Num}}}}}}]
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

(s/defschema Response {:id s/Str})

(defn schema-name [m]
  (-> m first val (subs (.length "#/definitions/"))))

(def valid-reference (just {:$ref anything}))

(defn extract-schema [swagger-spec uri path f]
  (let [operation (get-in swagger-spec [:paths uri :post])
        definitions (:definitions swagger-spec)
        schema (get-in operation (conj path :schema))
        name (schema-name (f schema))]
    {:name name
     :schema schema
     :defined? (boolean (definitions name))}))

(facts "transforming subschemas"
  (let [model {:id s/Str}
        swagger {:paths {"/resp" {:post {:responses {200 {:schema Response}
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

    (tabular
      (fact "body and response schemas in all flavours"
        (let [{:keys [name schema defined?]} (extract-schema spec ?uri ?path ?fn)]
          schema => ?schema
          name => ?name
          defined? => true))

      ?uri ?path ?fn ?name ?schema

      ;; body models
      "/body1" [:parameters 0] identity #"Body.*" valid-reference
      "/body2" [:parameters 0] :items   #"Body.*" (just {:items valid-reference, :type "array"})
      "/body3" [:parameters 0] :items   #"Body.*" (just {:items valid-reference, :type "array", :uniqueItems true})
      "/body4" [:parameters 0] identity #"Body.*" valid-reference
      "/body5" [:parameters 0] :items   #"Body.*" (just {:items valid-reference, :type "array"})
      "/body6" [:parameters 0] :items   #"Body.*" (just {:items valid-reference, :type "array", :uniqueItems true})

      ;; response models
      "/resp" [:responses 200] identity "Response"    valid-reference
      "/resp" [:responses 201] :items   #"Response.*" (just {:items valid-reference, :type "array"})
      "/resp" [:responses 202] :items   #"Response.*" (just {:items valid-reference, :type "array", :uniqueItems true})
      "/resp" [:responses 203] identity #"Response.*" valid-reference
      "/resp" [:responses 204] :items   #"Response.*" (just {:items valid-reference, :type "array"})
      "/resp" [:responses 205] :items   #"Response.*" (just {:items valid-reference, :type "array", :uniqueItems true}))))

(fact "multiple different schemas with same name"
  (let [model1 (s/schema-with-name {:id s/Str} 'Kikka)
        model2 (s/schema-with-name {:id s/Int} 'Kikka)
        swagger {:paths {"/body" {:post {:parameters {:body {:1 model1, :2 model2}}}}}}]
    (validate swagger) => (throws IllegalArgumentException)))

(defn has-definition [schema-name value]
  (chatty-checker [actual]
    (= (get-in actual [:definitions (name schema-name)]) value)))

(fact "additionalProperties"
  (let [Kikka  (s/schema-with-name {:a s/Str s/Keyword s/Str} 'Kikka)
        Kukka  (s/schema-with-name {:a s/Str s/Keyword Kikka} 'Kukka)
        Kakka  (s/schema-with-name {s/Keyword Kukka} 'Kakka)
        swagger {:paths {"/kikka" {:post {:parameters {:body Kikka}}}
                         "/kukka" {:post {:parameters {:body Kukka}}}
                         "/kakka" {:post {:parameters {:body Kakka}}}}}
        spec (swagger-json swagger)]
    (validate swagger) => nil

    (fact "keyword to primitive mapping"
      spec => (has-definition
                'Kikka
                {:properties {:a {:type "string"}}
                 :additionalProperties {:type "string"}
                 :required [:a]}))

    (fact "keyword to model mapping"
      spec => (has-definition
                'Kukka
                {:properties {:a {:type "string"}}
                 :additionalProperties {:$ref "#/definitions/Kikka"}
                 :required [:a]}))

    (fact "just additional properties"
      spec => (has-definition
                'Kakka
                {:additionalProperties {:$ref "#/definitions/Kukka"}}))))

(fact "tags"
  (let [swagger {:tags [{:name "pet"
                         :description "Everything about your Pets"
                         :externalDocs {:description "Find out more"
                                        :url "http://swagger.io"}}
                        {:name "store"
                         :description "Operations about user"}]
                 :paths {"/pet"   {:post {:tags ["pet"]}}
                         "/store" {:post {:tags ["store"]}}}}]
    (validate swagger) => nil))
