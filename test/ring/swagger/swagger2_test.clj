(ns ring.swagger.swagger2-test
  (:require [schema.core :as s]
            [ring.swagger.swagger2 :as swagger2]
            [ring.swagger.swagger2-full-schema :as full-schema]
            [ring.swagger.json-schema :as rsjs]
            [ring.swagger.extension :as extension]
            [ring.swagger.validator :as validator]
            [linked.core :as linked]
            [ring.util.http-status :as status]
            [midje.sweet :refer :all])
  (:import [java.util Date UUID]
           [java.util.regex Pattern]
           [org.joda.time DateTime LocalDate LocalTime]
           (java.io File)))

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
    (validator/validate (swagger2/swagger-json swagger options))))

(defn validate [swagger & [options]]
  (s/with-fn-validation
    (if-let [input-errors (s/check swagger2/Swagger swagger)]
      {:input-errors input-errors}
      (if-let [output-errors (validate-swagger-json swagger options)]
        {:output-errors output-errors}))))

(def a-complete-swagger
  {:swagger "2.0"
   :info {:version "version"
          :title "title"
          :description "description"
          :termsOfService "jeah"
          :contact {:name "name"
                    :url "http://someurl.com"
                    :email "tommi@example.com"}
          :license {:name "name"
                    :url "http://someurl.com"}
          :x-kikka "jeah"}
   :host "somehost:8080"
   :externalDocs {:url "http://someurl.com"
                  :description "more info"}
   :basePath "/"
   :schemes []
   :parameters {}
   :responses {}
   :securityDefinitions {}
   :security []
   :tags [{:name "pet",
           :description "Everything about your Pets",
           :externalDocs {:description "Find out more", :url "http://swagger.io"}}
          {:name "store",
           :description "Access to Petstore orders"}
          {:name "user",
           :description "Operations about user",
           :externalDocs {:description "Find out more about our store", :url "http://swagger.io"}}]
   :consumes ["application/json" "application/edn"]
   :produces ["application/json" "application/edn"]
   :paths {"/api/:id" {:get {:tags ["pet"]
                             :summary "summary"
                             :description "description"
                             :externalDocs {:url "http://someurl.com"
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
                                                           :k2 LocalTime
                                                           :l (s/maybe String)
                                                           :m (s/both Long (s/pred odd? 'odd?))
                                                           :o [{:p #{{:q String}}}]
                                                           :u UUID
                                                           :v Pattern
                                                           :w #"a[6-9]"}}}
                                   :responses {200 {:description "file"
                                                    :schema File}}}}
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
                                            :query (merge Anything {:x Long :y Long})}
                               :responses {200 {:schema {:sum Long}}
                                           :default {:schema {:code Long}
                                                     :headers {:location String}}}}
                        :put {:parameters {:body [(s/maybe Pet)]
                                           :query {:x (s/maybe String)}}
                              :responses {200 {:description "ok"
                                               :schema {:sum (s/maybe Long)}}
                                          :default {:description "error"}}}}
           "/api/turtle" {:get {:parameters {:body Turtle
                                             :query (merge Anything {:x Long :y Long})
                                             :path Nothing
                                             :header Anything
                                             :formData Anything}
                                :responses {200 {:description "ok"
                                                 :schema {:sum Long}}
                                            :default {:description "error"
                                                      :schema {:code Long}}}}
                          :post {:parameters {:body #{Turtle}
                                              :query (merge Anything {:x Long :y Long})}
                                 :responses {200 {:schema {:sum Long}}
                                             :default {:schema {:code Long}
                                                       :headers {:location String}}}}
                          :put {:parameters {:body [(s/maybe Turtle)]
                                             :query {:x (s/maybe String)}}
                                :responses {200 {:description "ok"
                                                 :schema {:sum (s/maybe Long)}}
                                            :default {:description "error"}}}}}})

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
  (validate a-complete-swagger) => nil)

(extension/java-time
  (fact "spec with java.time"
    (let [model {:i java.time.Instant
                 :ld java.time.LocalDate
                 :lt java.time.LocalTime}
          swagger {:paths {"/time" {:post {:parameters {:body model}}}}}]
      (validate swagger) => nil)))

(defrecord InvalidElement [])

(facts "with missing schema -> json schema mappings"

  (fact "non-body-parameters"
    (let [swagger {:paths {"/hello" {:get {:parameters {:query {:name InvalidElement}}}}}}]

      (fact "dy default, exception is throws when generating json schema"
        (validate swagger) => (throws IllegalArgumentException))

      (fact "with :ignore-missing-mappings errors (and mappings) are ignored"
        (validate swagger {:ignore-missing-mappings? true}) => nil)))

  (fact "body-parameters"
    (let [swagger {:paths {"/hello" {:post {:parameters {:body {:name InvalidElement
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

      (swagger2/swagger-json swagger)
      => (contains {:paths
                    {"/hello"
                     {:post
                      {:responses
                       {200 {:description ""}
                        425 {:description ""}
                        500 {:description "FAIL"}}}}}}))

    (fact ":default-response-description-fn option overriden"
      (let [options {:default-response-description-fn status/get-description}]
        (validate swagger options) => nil

        (swagger2/swagger-json swagger options)
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
(def valid-optional-reference (just {:$ref anything :x-nullable true}))

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
        spec (swagger2/swagger-json swagger)]

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
      "/body2" [:parameters 0] :items #"Body.*" (just {:items valid-reference, :type "array"})
      "/body3" [:parameters 0] :items #"Body.*" (just {:items valid-reference, :type "array", :uniqueItems true})
      "/body4" [:parameters 0] identity #"Body.*" valid-optional-reference
      "/body5" [:parameters 0] :items #"Body.*" (just {:items valid-optional-reference, :type "array"})
      "/body6" [:parameters 0] :items #"Body.*" (just {:items valid-optional-reference, :type "array", :uniqueItems true})

      ;; response models
      "/resp" [:responses 200] identity "Response" valid-reference
      "/resp" [:responses 201] :items #"Response.*" (just {:items valid-reference, :type "array"})
      "/resp" [:responses 202] :items #"Response.*" (just {:items valid-reference, :type "array", :uniqueItems true})
      "/resp" [:responses 203] identity #"Response.*" valid-optional-reference
      "/resp" [:responses 204] :items #"Response.*" (just {:items valid-optional-reference, :type "array"})
      "/resp" [:responses 205] :items #"Response.*" (just {:items valid-optional-reference, :type "array", :uniqueItems true}))))

(fact "multiple different schemas with same name"
  (let [model1 (s/schema-with-name {:id s/Str} 'Kikka)
        model2 (s/schema-with-name {:id s/Int} 'Kikka)
        swagger {:paths {"/body" {:post {:parameters {:body {:1 model1, :2 model2}}}}}}]

    (fact "with default options"
      (validate swagger)
      => nil)

    (fact "with overriden options"
      (validate swagger {:handle-duplicate-schemas-fn ring.swagger.core/fail-on-duplicate-schema!})
      => (throws IllegalArgumentException))))

(defn has-definition [schema-name value]
  (chatty-checker [actual]
                  (= (get-in actual [:definitions (name schema-name)]) value)))

(fact "additionalProperties"
  (let [Kikka (s/schema-with-name {:a s/Str s/Str s/Str} 'Kikka)
        Kukka (s/schema-with-name {:a s/Str s/Int Kikka} 'Kukka)
        Kakka (s/schema-with-name {s/Keyword Kukka} 'Kakka)
        swagger {:paths {"/kikka" {:post {:parameters {:body Kikka}}}
                         "/kukka" {:post {:parameters {:body Kukka}}}
                         "/kakka" {:post {:parameters {:body Kakka}}}}}
        spec (swagger2/swagger-json swagger)]
    (validate swagger) => nil

    (fact "keyword to primitive mapping"
      spec => (has-definition
                'Kikka
                {:type "object"
                 :properties {:a {:type "string"}}
                 :additionalProperties {:type "string"}
                 :required [:a]}))

    (fact "keyword to model mapping"
      spec => (has-definition
                'Kukka
                {:type "object"
                 :properties {:a {:type "string"}}
                 :additionalProperties {:$ref "#/definitions/Kikka"}
                 :required [:a]}))

    (fact "just additional properties"
      spec => (has-definition
                'Kakka
                {:type "object"
                 :additionalProperties {:$ref "#/definitions/Kukka"}}))))

(fact "extra meta-data to properties"
  (let [Kikka (s/schema-with-name {:a (rsjs/field s/Str {:description "A"})
                                   :b (rsjs/field [s/Str] {:description "B"})} 'Kikka)
        swagger {:paths {"/kikka" {:post {:parameters {:body Kikka}}}}}
        spec (swagger2/swagger-json swagger)]

    (validate swagger) => nil

    spec => (has-definition
              'Kikka
              {:type "object"
               :properties {:a {:type "string"
                                :description "A"}
                            :b {:type "array"
                                :items {:type "string"}
                                :description "B"}}
               :additionalProperties false
               :required [:a :b]})))

(fact "tags"
  (let [swagger {:tags [{:name "pet"
                         :description "Everything about your Pets"
                         :externalDocs {:description "Find out more"
                                        :url "http://swagger.io"}}
                        {:name "store"
                         :description "Operations about user"}]
                 :paths {"/pet" {:post {:tags ["pet"]}}
                         "/store" {:post {:tags ["store"]}}}}]
    (validate swagger) => nil))

(fact "retain :paths order, #54"
  (let [->path (fn [x] (str "/" x))
        paths (reduce (fn [acc x] (assoc acc (->path x) {:get {}})) (linked/map) (range 100))
        swagger {:paths paths}
        spec (swagger2/swagger-json swagger)]
    (-> spec :paths keys) => (map ->path (range 100))))

(fact "transform-operations"
  (let [remove-x-no-doc (fn [endpoint] (if-not (some-> endpoint :x-no-doc true?) endpoint))
        swagger {:paths {"/a" {:get {:x-no-doc true}, :post {}}
                         "/b" {:put {:x-no-doc true}}}}]
    (swagger2/transform-operations remove-x-no-doc swagger) => {:paths {"/a" {:post {}}}}

    (swagger2/transform-operations remove-x-no-doc {:paths {"/a" {:get {:x-no-doc true}, :post {}}
                                                            "/b" {:put {:x-no-doc true}}}})))

(s/defschema SchemaA {:a s/Str})
(s/defschema SchemaB {:b s/Str})

(fact "s/either stuff is correctly named"
  (-> (swagger2/swagger-json {:paths {"/ab" {:get {:parameters {:body (s/either SchemaA SchemaB)}}}}})
      (get-in [:paths "/ab" :get :parameters 0]))
  => {:in "body"
      :name "SchemaA"
      :description ""
      :required true
      :schema {:$ref "#/definitions/SchemaA"}})

(fact "body wrapped in Maybe make's it optional"
  (-> (swagger2/swagger-json {:paths {"/maybe" {:post {:parameters {:body (s/maybe {:kikka s/Str})}}}}})
      (get-in [:paths "/maybe" :post :parameters 0]))
  => (contains {:in "body", :required false}))

(fact "path-parameters with .dot extension, #82"
  (swagger2/swagger-json
    {:paths {"/api/:id.json" {:get {:parameters {:path {:id String}}}}}})
  => (contains
       {:paths (just
                 {"/api/{id}.json" (contains
                                     {:get (contains
                                             {:parameters (just
                                                            [(contains
                                                               {:name "id"})])})})})}))

(fact "should validate full swagger 2 schema"
  (s/validate full-schema/Swagger a-complete-swagger) => a-complete-swagger)

(s/defschema Required
  (rsjs/field
    {(s/optional-key :name) s/Str
     (s/optional-key :title) s/Str
     :address (rsjs/field
                {:street (rsjs/field s/Str {:description "description here"})}
                {:description "Streename"
                 :example "Ankkalinna 1"})}
    {:minProperties 1
     :description "I'm required"
     :example {:name "Iines"
               :title "Ankka"}}))

(fact "models with extra meta, #96"
  (let [swagger {:paths {"/api" {:post {:parameters {:body Required}}}}}]

    (swagger2/swagger-json swagger)
    => (contains
         {:definitions {"Required" {:type "object"
                                    :description "I'm required"
                                    :example {:name "Iines"
                                              :title "Ankka"}
                                    :minProperties 1
                                    :required [:address]
                                    :properties {:name {:type "string"}
                                                 :title {:type "string"}
                                                 :address {:$ref "#/definitions/RequiredAddress"}}
                                    :additionalProperties false}
                        "RequiredAddress" {:type "object"
                                           :description "Streename"
                                           :example "Ankkalinna 1"
                                           :properties {:street {:type "string"
                                                                 :description "description here"}}
                                           :required [:street]
                                           :additionalProperties false}}
          :paths {"/api" {:post {:parameters [{:description "I'm required"
                                               :in "body"
                                               :name "Required"
                                               :required true
                                               :schema {:$ref "#/definitions/Required"}}]
                                 :responses {:default {:description ""}}}}}})

    (validate swagger) => nil))

(s/defschema OptionalMaybe
  {(s/optional-key :a) s/Str
   (s/optional-key :b) (s/maybe s/Str)
   :c (s/maybe s/Str)})

(fact "nillable fields, #97"
  (let [swagger {:paths {"/api" {:post {:parameters {:body (s/maybe OptionalMaybe)}}}}}]

    (swagger2/swagger-json swagger)
    => (contains
         {:definitions {"OptionalMaybe" {:type "object"
                                         :properties {:a {:type "string"}
                                                      :b {:type "string" :x-nullable true}
                                                      :c {:type "string" :x-nullable true}}
                                         :required [:c]
                                         :additionalProperties false}}
          :paths {"/api" {:post {:parameters [{:in "body"
                                               :name "OptionalMaybe"
                                               :description ""
                                               :required false
                                               :schema {:$ref "#/definitions/OptionalMaybe"
                                                        :x-nullable true}}]
                                 :responses {:default {:description ""}}}}}})

    (validate swagger) => nil))

(s/defrecord Keyboard [type :- (s/enum :left :right)])
(s/defrecord User [age :- s/Int, keyboard :- Keyboard])

(fact "top-level & nested records are embedded"
  (let [swagger {:paths {"/api" {:post {:parameters {:body User}}}}}]
    (swagger2/swagger-json swagger)
    => (contains
         {:definitions {}
          :paths
          {"/api"
           {:post
            {:parameters
             [{:in "body"
               :name "User"
               :description ""
               :required true
               :schema {:type "object"
                        :title "User"
                        :properties {:age {:format "int64"
                                           :type "integer"}
                                     :keyboard {:type "object"
                                                :title "Keyboard"
                                                :properties {:type {:type "string"
                                                                    :enum [:right :left]}}
                                                :required [:type]
                                                :additionalProperties false}}
                        :required [:age :keyboard]
                        :additionalProperties false}}]
             :responses {:default {:description ""}}}}}})

    (validate swagger) => nil))

(fact "primitive vector body parameters & responses"
  (let [swagger {:paths {"/api" {:post {:parameters {:body [s/Str]}
                                        :responses {200 {:schema [s/Str]}}}}}}]
    (swagger2/swagger-json swagger)
    => (contains
         {:definitions {}
          :paths {"/api" {:post {:parameters [{:in "body"
                                               :name ""
                                               :description ""
                                               :required true
                                               :schema {:items {:type "string"}
                                                        :type "array"}}]
                                 :responses {200 {:description ""
                                                  :schema {:items {:type "string"}
                                                           :type "array"}}}}}}})

    (validate swagger) => nil))

(fact "primitive set body parameters & responses"
  (let [swagger {:paths {"/api" {:post {:parameters {:body #{s/Str}}
                                        :responses {200 {:schema #{s/Str}}}}}}}]
    (swagger2/swagger-json swagger)
    => (contains
         {:definitions {}
          :paths {"/api" {:post {:parameters [{:in "body"
                                               :name ""
                                               :description ""
                                               :required true
                                               :schema {:items {:type "string"}
                                                        :uniqueItems true
                                                        :type "array"}}]
                                 :responses {200 {:description ""
                                                  :schema {:items {:type "string"}
                                                           :uniqueItems true
                                                           :type "array"}}}}}}})

    (validate swagger) => nil))

(fact "primitive body parameters & responses"
  (let [swagger {:paths {"/api" {:post {:parameters {:body s/Str}
                                        :responses {200 {:schema s/Str}}}}}}]
    (swagger2/swagger-json swagger)
    => (contains
         {:definitions {}
          :paths {"/api" {:post {:parameters [{:in "body"
                                               :name ""
                                               :description ""
                                               :required true
                                               :schema {:type "string"}}]
                                 :responses {200 {:description ""
                                                  :schema {:type "string"}}}}}}})

    (validate swagger) => nil))

(s/defschema Person {:age s/Int})

(def Adult (s/constrained Person #(>= (:age %) 18)))

(fact "query parameters with ..., #104"

  (fact "constrained schema"
    (let [swagger {:paths {"/people" {:get {:parameters {:query Adult}
                                            :responses {200 {:schema s/Str}}}}}}]
      (swagger2/swagger-json swagger)
      => (contains
           {:definitions {}
            :paths {"/people" {:get {:parameters [{:in "query"
                                                   :name "age"
                                                   :description ""
                                                   :required true
                                                   :type "integer"
                                                   :format "int64"}]
                                     :responses {200 {:description ""
                                                      :schema {:type "string"}}}}}}})
      (validate swagger) => nil))
  (fact "top-level maybe schema"
    (let [swagger {:paths {"/" {:get {:parameters {:query (s/maybe {:a s/Str})}}}}}]
      (swagger2/swagger-json swagger)
      => (contains
          {:definitions {}
           :paths {"/" {:get {:parameters [{:in "query"
                                            :name "a"
                                            :description ""
                                            :required true
                                            :type "string"}]
                              :responses {:default {:description ""}}}}}})
      (validate swagger) => nil))
  (fact "nested maybe schema"
    (let [swagger {:paths {"/" {:get {:parameters {:query {:a (s/maybe s/Str)}}}}}}]
      (swagger2/swagger-json swagger)
      => (contains
           {:definitions {}
            :paths {"/" {:get {:parameters [{:in "query"
                                             :name "a"
                                             :description ""
                                             :required true
                                             :type "string"
                                             :allowEmptyValue true}]
                               :responses {:default {:description ""}}}}}})
          (validate swagger) => nil)))

(fact "qualified keywords"
  (let [swagger {:paths {"/any" {:get {:parameters {:query {:kikka/kukka String}}
                                       :responses {:default {:schema {:olipa/kerran String}}}}}}}
        endpoint (get-in (swagger2/swagger-json swagger) [:paths "/any" :get])
        response (-> (swagger2/swagger-json swagger) :definitions first second)]

    (get-in endpoint [:parameters 0]) => (contains {:name "kikka/kukka"})
    (get-in response [:properties]) => (contains {:olipa/kerran {:type "string"}})))


(fact "Meta-description of body responses are considered"
  (let [swagger {:paths {"/meta" {:get {:parameters {:query {:kikka/kukka String}}
                                       :responses {200 (rsjs/describe {:body {:kikka/kukka String}}
                                                                      "A meta description")}}}
                         "/direct" {:get {:parameters {:query {:kikka/kukka String}}
                                          :responses {200 {:body {:kikka/kukka String}
                                                           :description "A direct description"}}}}}}]
    (get-in (swagger2/swagger-json swagger) [:paths "/meta" :get :responses 200]) => (contains {:description "A meta description"})
    (get-in (swagger2/swagger-json swagger) [:paths "/direct" :get :responses 200]) => (contains {:description "A direct description"})))
