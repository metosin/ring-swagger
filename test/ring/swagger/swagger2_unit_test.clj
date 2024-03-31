(ns ring.swagger.swagger2-unit-test
  (:require [midje.sweet :refer :all]
            [schema.core :as s]
            [ring.swagger.json-schema :as rsjs]
            [ring.swagger.core :as rsc]
            [ring.swagger.swagger2 :as swagger2]))

;;
;; Schemas
;;

(s/defschema Tag
  {(s/optional-key :id) (rsjs/field s/Int {:description "Unique identifier for the tag"})
   (s/optional-key :name) (rsjs/field s/Str {:description "Friendly name for the tag"})})

(s/defschema Category
  {(s/optional-key :id) (rsjs/field s/Int {:description "Category unique identifier"
                                           :minimum "0.0"
                                           :maximum "100.0"})
   (s/optional-key :name) (rsjs/field s/Str {:description "Name of the category"})})

(s/defschema Pet
  {:id (rsjs/field s/Int {:description "Unique identifier for the Pet"
                          :minimum "0.0"
                          :maximum "100.0"})
   :name (rsjs/field s/Str {:description "Friendly name of the pet"})
   (s/optional-key :category) Category
   (s/optional-key :photoUrls) (rsjs/field [s/Str] {:description "Image URLs"})
   (s/optional-key :tags) (rsjs/field [Tag] {:description "Tags assigned to this pet"})
   (s/optional-key :status) (rsjs/field (s/enum :available :pending :sold) {:description "pet status in the store"})})

(s/defschema PetError {:message String s/Keyword s/Any})

(def +options+ swagger2/option-defaults)

;;
;; Excepcted JSON Schemas
;;

(def Tag'
  {:type "object"
   :properties {:id {:type "integer"
                     :format "int64"
                     :description "Unique identifier for the tag"}
                :name {:type "string"
                       :description "Friendly name for the tag"}}
   :additionalProperties false})

(def Category'
  {:type "object"
   :properties {:id {:type "integer"
                     :format "int64"
                     :description "Category unique identifier"
                     :minimum "0.0"
                     :maximum "100.0"}
                :name {:type "string"
                       :description "Name of the category"}}
   :additionalProperties false})

(def Pet'
  {:type "object"
   :required [:id :name]
   :properties {:id {:type "integer"
                     :format "int64"
                     :description "Unique identifier for the Pet"
                     :minimum "0.0"
                     :maximum "100.0"}
                :category {:$ref "#/definitions/Category"}
                :name {:type "string"
                       :description "Friendly name of the pet"}
                :photoUrls {:type "array"
                            :description "Image URLs"
                            :items {:type "string"}}
                :tags {:type "array"
                       :description "Tags assigned to this pet"
                       :items {:$ref "#/definitions/Tag"}}
                :status {:type "string"
                         :description "pet status in the store"
                         :enum [:pending :sold :available]}}
   :additionalProperties false})

;;
;; Facts
;;

(fact "transform simple schemas"
  (rsjs/schema-object Tag :swagger) => Tag'
  (rsjs/schema-object Category :swagger) => Category'
  (rsjs/schema-object Pet :swagger) => Pet')

(s/defschema RootModel
  {:sub {:foo Long}})

(fact "collect-models"
  (fact "Sub-schemas are collected"
    (rsc/collect-models Pet)
    => {'Pet #{Pet}
        'Tag #{Tag}
        'Category #{Category}})

  (fact "No schemas are collected if all are unnamed"
    (rsc/collect-models String) => {})

  (fact "Inline-sub-schemas as collected after they are nameed"
    (rsc/collect-models (rsc/with-named-sub-schemas RootModel))
    => {'RootModel #{RootModel}
        'RootModelSub #{(:sub RootModel)}})

  (fact "Described anonymous models are collected"
    (let [schema (rsjs/describe {:sub (rsjs/describe {:foo Long} "the sub schema")} "the root schema")]
      (keys (rsc/collect-models (rsc/with-named-sub-schemas schema))) => (two-of symbol?))))

(fact "transform-models"
  (swagger2/transform-models [Pet] +options+) => {"Pet" Pet'
                                                  "Tag" Tag'
                                                  "Category" Category'})

(s/defschema Foo (s/enum :a :b))
(s/defschema Bar {:key Foo})
(s/defschema Baz s/Keyword)

(fact "record-schemas are not transformed"
  (swagger2/transform-models [Foo] +options+) => {})

(fact "non-map schemas are not transformed"
  (swagger2/transform-models [Baz] +options+) => {})

(fact "nested record-schemas are inlined"
  (swagger2/transform-models [Bar] +options+) => {"Bar" {:type "object"
                                                         :properties {:key {:enum [:b :a]
                                                                            :type "string"}}
                                                         :additionalProperties false
                                                         :required [:key]}})

(fact "nested schemas"

  (fact "with anonymous sub-schemas"
    (s/defschema Nested {:id s/Str
                         :address {:country (s/enum :fi :pl)
                                   :street {:name s/Str}}})
    (swagger2/transform-models [(rsc/with-named-sub-schemas Nested)] +options+)

    => {"Nested" {:type "object"
                  :properties {:address {:$ref "#/definitions/NestedAddress"}
                               :id {:type "string"}}
                  :additionalProperties false
                  :required [:id :address]}
        "NestedAddress" {:type "object"
                         :properties {:country {:enum [:fi :pl]
                                                :type "string"}
                                      :street {:$ref "#/definitions/NestedAddressStreet"}}
                         :additionalProperties false
                         :required [:country :street]}
        "NestedAddressStreet" {:type "object"
                               :properties {:name {:type "string"}}
                               :additionalProperties false
                               :required [:name]}})

  (fact "nested named sub-schemas"

    (s/defschema Boundary
      {:type (s/enum "MultiPolygon" "Polygon" "MultiPoint" "Point")
       :coordinates [s/Any]})

    (s/defschema ReturnValue
      {:boundary (s/maybe Boundary)})

    (keys
      (swagger2/transform-models
        [(rsc/with-named-sub-schemas ReturnValue)]
        +options+)) => ["Boundary" "ReturnValue"]))

(s/defschema Query {:id Long (s/optional-key :q) String})
(s/defschema Path {:p Long})
(s/defschema Body {:name String :age Long})

(fact "convert-parameters"

  (fact "all parameter types can be converted"
    (swagger2/convert-parameters
      {:body Pet
       :query Query
       :path Path
       :header {:h String}
       :formData {:f Integer}} {})

    => [{:name "Pet"
         :in "body"
         :description ""
         :required true
         :schema {:$ref "#/definitions/Pet"}}
        {:name "id"
         :in "query"
         :description ""
         :required true
         :type "integer"
         :format "int64"}
        {:name "q"
         :in "query"
         :description ""
         :required false
         :type "string"}
        {:name "p"
         :in "path"
         :description ""
         :required true
         :type "integer"
         :format "int64"}
        {:name "h"
         :in "header"
         :description ""
         :required true
         :type "string"}
        {:name "f"
         :in "formData"
         :description ""
         :required true
         :type "integer"
         :format "int32"}])

  (fact "schemas wtih postconditions are supported in ..."
    (s/defschema Polygon {:sides s/Int})
    (s/defschema Triangle (s/constrained Polygon #(= (:sides %) 3)))

    (fact ":query parameters"
      (swagger2/convert-parameters
        {:query Triangle} {})
      => [{:name "sides"
           :in "query"
           :description ""
           :required true
           :type "integer"
           :format "int64"}])

    (fact ":body parameters"
      (swagger2/convert-parameters
        {:body Triangle} {})
      => [{:name "Triangle"
           :in "body"
           :description ""
           :required true
           :schema {:$ref "#/definitions/Polygon"}}]))

  (fact "anonymous schemas can be used with ..."

    (fact ":query-parameters"
      (swagger2/convert-parameters
        {:query {s/Keyword s/Any
                 :q String
                 (s/optional-key :l) Long}} {})

      => [{:name "q"
           :description ""
           :in "query"
           :required true
           :type "string"}
          {:name "l"
           :description ""
           :format "int64"
           :in "query"
           :required false
           :type "integer"}])

    (fact ":path-parameters (are always required)"
      (swagger2/convert-parameters
        {:path {s/Keyword s/Any
                :q String
                (s/optional-key :l) Long}} {})

      => [{:name "q"
           :description ""
           :in "path"
           :required true
           :type "string"}
          {:name "l"
           :description ""
           :format "int64"
           :in "path"
           :required true
           :type "integer"}]))

  (fact "Array body parameters"
    (swagger2/convert-parameters {:body [Body]} {})

    => [{:name "Body"
         :description ""
         :in "body"
         :required true
         :schema {:type "array"
                  :items {:$ref "#/definitions/Body"}}}])

  (fact "Set body parameters"
    (swagger2/convert-parameters {:body #{Body}} {})

    => [{:name "Body"
         :description ""
         :in "body"
         :required true
         :schema {:type "array"
                  :uniqueItems true
                  :items {:$ref "#/definitions/Body"}}}])

  (fact "Body param with desc"
    (swagger2/convert-parameters {:body (rsjs/describe Body "foo")} {})

    => [{:description "foo"
         :name "Body"
         :in "body"
         :required true
         :schema {:$ref "#/definitions/Body"}}])

  (fact "Array body param with desc"
    (swagger2/convert-parameters {:body [(rsjs/describe Body "foo")]} {})

    => [{:description "foo"
         :name "Body"
         :in "body"
         :required true
         :schema {:type "array"
                  :items {:$ref "#/definitions/Body"}}}]))

(fact "ensure-named-top-level-models"

  (let [[paths definitions] (swagger2/extract-paths-and-definitions
                              (swagger2/ensure-body-and-response-schema-names
                                {:paths {"/api" {:post {:parameters {:body {:foo s/Str}}
                                                        :responses {200 {:description "ok"
                                                                         :schema [{:bar Long}]}}}}}})
                              +options+)]

    (fact "anonymous map as body parameter is named and refers to correct definition"
      (let [body-schema-ref (-> paths
                                (get-in ["/api" :post :parameters])
                                first
                                (get-in [:schema :$ref]))
            body-schema-name (last (re-find #"\#\/definitions\/(.+)$" body-schema-ref))]
        (get definitions body-schema-name))

      => {:type "object"
          :properties {:foo {:type "string"}}
          :additionalProperties false
          :required [:foo]})

    (fact "array of anonymous map as response model is named and refers to correct definition"
      (let [response-schema-ref (-> paths
                                    (get-in ["/api" :post :responses 200 :schema :items :$ref]))
            response-schema-name (last (re-find #"\#\/definitions\/(.+)$" response-schema-ref))]
        (get definitions response-schema-name))

      => {:type "object"
          :properties {:bar {:type "integer"
                             :format "int64"}}
          :additionalProperties false
          :required [:bar]})))

;;
;; Helpers
;;

(fact "swagger-path"
  (swagger2/swagger-path "/api/:kikka/:kakka/:kukka") => "/api/{kikka}/{kakka}/{kukka}"
  (swagger2/swagger-path "/api/:id.json") => "/api/{id}.json"

  ;; From Clout tests

  (fact "keywords-match-extensions"
    (swagger2/swagger-path "/foo.:ext") => "/foo.{ext}"
    (swagger2/swagger-path "/:x.:y") => "/{x}.{y}")

  (fact "hyphen-keywords"
    (swagger2/swagger-path "/:foo-bar") => "/{foo-bar}"
    (swagger2/swagger-path "/:foo-") => "/{foo-}")

  (fact "underscore-keywords"
    (swagger2/swagger-path "/:foo_bar") => "/{foo_bar}"
    (swagger2/swagger-path "/:_foo") => "/{_foo}")

  (fact "non-ascii-keywords"
    (swagger2/swagger-path "/:äñßOÔ") => "/{äñßOÔ}"
    (swagger2/swagger-path "/:ÁäñßOÔ") => "/{ÁäñßOÔ}"
    (swagger2/swagger-path "/:ä/:ش") => "/{ä}/{ش}"
    (swagger2/swagger-path "/:ä/:ä") => "/{ä}/{ä}"
    (swagger2/swagger-path "/:Ä-ü") => "/{Ä-ü}"
    (swagger2/swagger-path "/:Ä_ü") => "/{Ä_ü}"))

(fact "extract-models"
  (fact "returns both return and body-parameters but not query or path parameter types"
    (swagger2/extract-models {:paths {"/foo" {:get {:parameters {:body Category
                                                                 :query Pet
                                                                 :path Pet}
                                                    :responses {200 {:schema Tag}}}}}})
    => [Category Tag]))


(declare Bar)

(s/defschema Foo {:bar (s/recursive #'Bar)})

(s/defschema Bar {:foo (s/maybe #'Foo)})

(fact "recursive"
  (swagger2/transform-models [Foo Bar] +options+)
  => {"Bar" {:type "object"
             :properties {:foo {:$ref "#/definitions/Foo"
                                :x-nullable true}}
             :additionalProperties false
             :required [:foo]}
      "Foo" {:type "object"
             :properties {:bar {:$ref "#/definitions/Bar"}}
             :additionalProperties false
             :required [:bar]}})

;;
;; Final json TODO: (duplicate tests to ring.swagger.swagger2-test?
;;

(facts "swagger json"
  (fact "without parameters"

    (swagger2/swagger-json {})
    => {:swagger "2.0"
        :info {:title "Swagger API"
               :version "0.0.1"}
        :produces ["application/json"]
        :consumes ["application/json"]
        :paths {}
        :definitions {}})

  (fact "full api"
    (swagger2/swagger-json
      {:swagger "2.0"
       :info {:version ..version..
              :title ..title..
              :description ..description1..
              :termsOfService ..terms..
              :contact {:name ..name1..
                        :url ..url1..
                        :email ..email1..}
              :license {:name ..name2..
                        :url ..url2..}
              :x-kikka ..kikka..}
       :basePath ..basepath..
       :consumes ["application/json" "application/edn"]
       :produces ["application/json" "application/edn"]
       :paths {"/api/path/:id" {:get {:tags [:tag1 :tag2 :tag3]
                                      :summary ..summary1..
                                      :description ..description2..
                                      :externalDocs {:url ..url3..
                                                     :description ..description3..}
                                      :operationId ..operationid..
                                      :consumes ["application/xyz"]
                                      :produces ["application/xyz"]
                                      :parameters {:path {:id Integer}}
                                      :responses {200 {:description "ok"
                                                       :schema Pet
                                                       :headers {"X-men" (rsjs/describe s/Str "mutant header")}}
                                                  404 {:description "fail"
                                                       :schema PetError}}}}}})
    => {:swagger "2.0"
        :info {:version ..version..
               :title ..title..
               :description ..description1..
               :termsOfService ..terms..
               :contact {:name ..name1..
                         :url ..url1..
                         :email ..email1..}
               :license {:name ..name2..
                         :url ..url2..}
               :x-kikka ..kikka..}
        :basePath ..basepath..
        :consumes ["application/json" "application/edn"]
        :produces ["application/json" "application/edn"]
        :paths {"/api/path/{id}" {:get {:tags [:tag1 :tag2 :tag3]
                                        :summary ..summary1..
                                        :description ..description2..
                                        :externalDocs {:url ..url3..
                                                       :description ..description3..}
                                        :operationId ..operationid..
                                        :consumes ["application/xyz"]
                                        :produces ["application/xyz"]
                                        :parameters [{:in "path"
                                                      :name "id"
                                                      :description ""
                                                      :required true
                                                      :type "integer"
                                                      :format "int32"}]
                                        :responses {200 {:description "ok"
                                                         :schema {:$ref "#/definitions/Pet"}
                                                         :headers {"X-men" {:type "string"
                                                                            :description "mutant header"}}}
                                                    404 {:description "fail"
                                                         :schema {:$ref "#/definitions/PetError"}}}}}}
        :definitions {"Pet" {:type "object"
                             :required [:id :name]
                             :properties {:id {:type "integer"
                                               :format "int64"
                                               :description "Unique identifier for the Pet"
                                               :minimum "0.0"
                                               :maximum "100.0"}
                                          :name {:type "string"
                                                 :description "Friendly name of the pet"}
                                          :category {:$ref "#/definitions/Category"}
                                          :photoUrls {:type "array"
                                                      :items {:type "string"}
                                                      :description "Image URLs"}
                                          :tags {:type "array"
                                                 :items {:$ref "#/definitions/Tag"}
                                                 :description "Tags assigned to this pet"}
                                          :status {:enum [:pending :sold :available]
                                                   :type "string"
                                                   :description "pet status in the store"}}
                             :additionalProperties false}
                      "Category" {:type "object"
                                  :properties {:id {:type "integer"
                                                    :format "int64"
                                                    :description "Category unique identifier"
                                                    :minimum "0.0"
                                                    :maximum "100.0"}
                                               :name {:type "string"
                                                      :description "Name of the category"}}
                                  :additionalProperties false}
                      "Tag" {:type "object"
                             :properties {:id {:type "integer"
                                               :format "int64"
                                               :description "Unique identifier for the tag"}
                                          :name {:type "string"
                                                 :description "Friendly name for the tag"}}
                             :additionalProperties false}
                      "PetError" {:type "object"
                                  :properties {:message {:type "string"}}
                                  :required [:message]
                                  :additionalProperties {}}}}))

(defrecord InvalidElement [])

(fact "transform schemas with missing mappings"

  (let [schema {:a String
                :b InvalidElement}]

    (fact "fail by default"
      (rsjs/schema-object schema :swagger) => (throws IllegalArgumentException))

    (fact "drops bad fields from both properties & required"
      (binding [rsjs/*ignore-missing-mappings* true]
        (rsjs/schema-object schema :swagger)

        => {:type "object"
            :properties {:a {:type "string"}}
            :additionalProperties false
            :required [:a]}))))

(fact "collectionFormat"
  (:collectionFormat (first (swagger2/convert-parameters {:query {:q [String]}} {})))
  => "multi"

  (:collectionFormat (first (swagger2/convert-parameters {:formData {:q [String]}} {})))
  => "multi"

  (:collectionFormat (first (swagger2/convert-parameters {:query {:q [String]}} {:collection-format "csv"})))
  => "csv")
