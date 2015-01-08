(ns ring.swagger.swagger2-unit-test
  (:require [midje.sweet :refer :all]
            [schema.core :as s]
            [ring.swagger.test-utils :refer :all]
            [ring.swagger.schema :refer :all]
            [ring.swagger.swagger2 :refer :all]
            [flatland.ordered.map :refer :all]))

;;
;; Schemas
;;

(s/defschema Tag {(s/optional-key :id)   (field s/Int {:description "Unique identifier for the tag"})
                  (s/optional-key :name) (field s/Str {:description "Friendly name for the tag"})})

(s/defschema Category {(s/optional-key :id)   (field s/Int {:description "Category unique identifier" :minimum "0.0" :maximum "100.0"})
                       (s/optional-key :name) (field s/Str {:description "Name of the category"})})

(s/defschema Pet {:id                         (field s/Int {:description "Unique identifier for the Pet" :minimum "0.0" :maximum "100.0"})
                  :name                       (field s/Str {:description "Friendly name of the pet"})
                  (s/optional-key :category)  (field Category {:description "Category the pet is in"})
                  (s/optional-key :photoUrls) (field [s/Str] {:description "Image URLs"})
                  (s/optional-key :tags)      (field [Tag] {:description "Tags assigned to this pet"})
                  (s/optional-key :status)    (field (s/enum :available :pending :sold) {:description "pet status in the store"})})

(s/defschema PetError {:message String s/Keyword s/Any})

(s/defschema OrderedSchema (ordered-map
                             :id Long
                             :hot Boolean
                             :tag (s/enum :kikka :kukka)
                             :chief [{:name String
                                      :type #{{:id String}}}]
                             :a String
                             :b String
                             :c String
                             :d String
                             :e String
                             :f String))

(def ordered-schema-order (keys OrderedSchema))

;;
;; Excepcted JSON Schemas
;;

(def Tag'
  {:properties {:id {:type "integer"
                     :format "int64"
                     :description "Unique identifier for the tag"}
                :name {:type "string"
                       :description "Friendly name for the tag"}}})

(def Category'
  {:properties {:id {:type "integer"
                     :format "int64"
                     :description "Category unique identifier"
                     :minimum "0.0"
                     :maximum "100.0"}
                :name {:type "string"
                       :description "Name of the category"}}})

(def Pet'
  {:required [:id :name]
   :properties {:id {:type "integer"
                     :format "int64"
                     :description "Unique identifier for the Pet"
                     :minimum "0.0"
                     :maximum "100.0"}
                :category {:$ref "#/definitions/Category"
                           :description "Category the pet is in"}
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
                         :enum [:pending :sold :available]}}})

;;
;; Facts
;;

(facts "simple schemas"
  (transform Tag) => Tag'
  (transform Category) => Category'
  (transform Pet) => Pet')

(s/defschema RootModel
  {:sub {:foo Long}})

(fact "with-named-sub-schemas"
  (fact "add :name meta-data to sub-schemas"
    (meta (:sub (with-named-sub-schemas RootModel))) => {:name 'RootModelSub})

  (fact "Keeps the order"
    (keys (with-named-sub-schemas OrderedSchema)) => ordered-schema-order))

(fact "collect-models"
  (fact "Sub-schemas are collected"
    (collect-models Pet)
    => {'Pet Pet
        'Tag Tag
        'Category Category})

  (fact "No schemas are collected if all are unnamed"
    (collect-models String) => {})

  (fact "Inline-sub-schemas as collected after they are named"
    (collect-models (with-named-sub-schemas RootModel))
    => {'RootModel RootModel
        'RootModelSub (:sub RootModel)})

  (fact "Described anonymous models are collected"
    (let [schema (describe {:sub (describe {:foo Long} "the sub schema")} "the root schema")]
      (keys (collect-models (with-named-sub-schemas schema))) => (two-of symbol?))))

(fact "transform-models"
  (transform-models [Pet]) => {:Pet Pet'
                               :Tag Tag'
                               :Category Category'}

  (s/defschema Foo (s/enum :a :b))
  (s/defschema Bar {:key Foo})
  (s/defschema Baz s/Keyword)

  (fact "record-schemas are not transformed"
    (transform-models [Foo]) => {})

  (fact "non-map schemas are not transformed"
    (transform-models [Baz]) => {})

  (fact "nested record-schemas are inlined"
    (transform-models [Bar]) => {:Bar {:properties {:key {:enum [:b :a]
                                                          :type "string"}}
                                       :required [:key]}})

  (fact "nested schemas"

    (fact "with anonymous sub-schemas"
      (s/defschema Nested {:id s/Str
                           :address {:country (s/enum :fi :pl)
                                     :street {:name s/Str}}})
      (transform-models [(with-named-sub-schemas Nested)])

      =>

      {:Nested {:properties {:address {:$ref "#/definitions/NestedAddress"}
                             :id {:type "string"}}
                :required [:id :address]}
       :NestedAddress {:properties {:country {:enum [:fi :pl]
                                              :type "string"}
                                    :street {:$ref "#/definitions/NestedAddressStreet"}}
                       :required [:country :street]}
       :NestedAddressStreet {:properties {:name {:type "string"}}
                             :required [:name]}})

    (fact "nested named sub-schemas"

      (s/defschema Boundary
        {:type (s/enum "MultiPolygon" "Polygon" "MultiPoint" "Point")
         :coordinates [s/Any]})

      (s/defschema ReturnValue
        {:boundary (s/maybe Boundary)})

      (keys
        (transform-models
          [(with-named-sub-schemas ReturnValue)])) => [:Boundary :ReturnValue])))

(s/defschema Query {:id Long (s/optional-key :q) String})
(s/defschema Path {:p Long})
(s/defschema Body {:name String :age Long})

(fact "convert-parameters"

  (fact "all parameter types can be converted"
    (convert-parameters
      {:body     Pet
       :query    Query
       :path     Path
       :header   {:h String}
       :formData {:f Integer}}) => [{:name       "Pet"
                                     :in          :body
                                     :description ""
                                     :required    true
                                     :schema      {:$ref "#/definitions/Pet"}}
                                    {:name        "id"
                                     :in          :query
                                     :description ""
                                     :required    true
                                     :type        "integer"
                                     :format      "int64"}
                                    {:name        "q"
                                     :in          :query
                                     :description ""
                                     :required    false
                                     :type        "string"}
                                    {:name        "p"
                                     :in          :path
                                     :description ""
                                     :required    true
                                     :type        "integer"
                                     :format      "int64"}
                                    {:name        "h"
                                     :in          :header
                                     :description ""
                                     :required    true
                                     :type        "string"}
                                    {:name        "f"
                                     :in          :formData
                                     :description ""
                                     :required    true
                                     :type        "integer"
                                     :format      "int32"}])

  (fact "anonymous schemas can be used with ..."

    (doseq [type [:query :path]]
      (fact {:midje/description (str "... " type "-parameters")}

        (convert-parameters
          {type {s/Keyword s/Any
                 :q String
                 (s/optional-key :l) Long}})

        => [{:name "q"
             :description ""
             :in type
             :required true
             :type "string"}
            {:name "l"
             :description ""
             :format "int64"
             :in type
             :required false
             :type "integer"}])))

  (fact "Array body parameters"
    (convert-parameters
      {:body [Body]})

    => [{:name "Body"
         :description ""
         :in :body
         :required true
         :schema {:type  "array"
                  :items {:$ref "#/definitions/Body"}}}])

  (fact "Set body parameters"
    (convert-parameters
      {:body #{Body}})

    => [{:name "Body"
         :description ""
         :in :body
         :required true
         :schema {:type        "array"
                  :uniqueItems true
                  :items       {:$ref "#/definitions/Body"}}}])


  (fact "Body param with desc"
    (convert-parameters {:body (describe Body "foo")})
    => [{:description "foo" :name "Body" :in :body :required true :schema {:$ref "#/definitions/Body"}}])

  (fact "Array body param with desc"
    (convert-parameters {:body [(describe Body "foo")]})
    => [{:description "foo" :name "Body" :in :body :required true :schema {:type "array"
                                                                           :items {:$ref "#/definitions/Body"}}}]))

(fact "ensure-named-top-level-models"

  (let [[paths definitions] (extract-paths-and-definitions
                             (ensure-named-top-level-models
                              {:paths {"/api" {:post {:parameters {:body {:foo s/Str}}
                                                      :responses  {200 {:description "ok"
                                                                        :schema [{:bar Long}]}}}}}}))]

    (fact "anonymous map as body parameter is named and refers to correct definition"
          (let [body-schema-ref  (-> paths
                                     (get-in ["/api" :post :parameters])
                                     first
                                     (get-in [:schema :$ref]))
                body-schema-name (last (re-find #"\#\/definitions\/(.+)$" body-schema-ref))]
            (get definitions (keyword body-schema-name))) => {:properties {:foo {:type "string"}}
                                                              :required   [:foo]})

    (fact "array of anonymous map as response model is named and refers to correct definition"
          (let [response-schema-ref  (-> paths
                                         (get-in ["/api" :post :responses 200 :schema :items :$ref]))
                response-schema-name (last (re-find #"\#\/definitions\/(.+)$" response-schema-ref))]
            (get definitions (keyword response-schema-name))) => {:properties {:bar {:type "integer"
                                                                                     :format "int64"}}
                                                                  :required   [:bar]})))

;; ;;
;; ;; Helpers
;; ;;

(fact "swagger-path"
  (swagger-path "/api/:kikka/:kakka/:kukka") => "/api/{kikka}/{kakka}/{kukka}")


(fact "extract-models"
  (fact "returns both return and body-parameters but not query or path parameter types"
    (extract-models {:paths {"/foo" {:get {:parameters {:body Category
                                                        :query Pet
                                                        :path Pet}
                                           :responses {200 {:schema Tag}}}}}})
    => [Category Tag]))


(declare Bar)

(s/defschema Foo {:bar (s/recursive #'Bar)})

(s/defschema Bar {:foo (s/maybe #'Foo)})

(fact "recursive"
  (collect-models [Foo Bar])
  => {'Bar {:foo (s/maybe #'Foo)}
      'Foo {:bar (s/recursive #'Bar)}}

  (transform-models [Foo Bar])
  => {:Bar {:properties {:foo {:$ref "#/definitions/Foo"}}
            :required [:foo]}
      :Foo {:properties {:bar {:$ref "#/definitions/Bar"}}
            :required [:bar]}})

;;
;; Final json TODO: (duplicate tests to ring.swagger.swagger2-test?
;;

(facts "swagger json"
  (fact "without parameters"
    (swagger-json {}) => {:swagger     "2.0"
                          :info        {:title "Swagger API"
                                        :version "0.0.1"}
                          :produces    ["application/json"]
                          :consumes    ["application/json"]
                          :paths       {}
                          :definitions {}})

  (fact "full api"
    (swagger-json
      {:swagger  "2.0"
       :info     {:version ..version..
                  :title ..title..
                  :description ..description1..
                  :termsOfService ..terms..
                  :contact {:name ..name1..
                            :url ..url1..
                            :email ..email1..}
                  :license {:name ..name2..
                            :url  ..url2..}
                  :x-kikka ..kikka..}
       :basePath ..basepath..
       :consumes ["application/json" "application/edn"]
       :produces ["application/json" "application/edn"]
       :paths    {"/api/path/:id" {:get {:tags         [:tag1 :tag2 :tag3]
                                         :summary      ..summary1..
                                         :description  ..description2..
                                         :externalDocs {:url ..url3..
                                                        :description ..description3..}
                                         :operationId  ..operationid..
                                         :consumes     ["application/xyz"]
                                         :produces     ["application/xyz"]
                                         :parameters   {:path {:id Integer}}
                                         :responses    {200 {:description "ok"
                                                             :schema Pet}
                                                        404 {:description "fail"
                                                             :schema PetError}}}}}})
    => {:swagger     "2.0"
        :info        {:version        ..version..
                      :title          ..title..
                      :description    ..description1..
                      :termsOfService ..terms..
                      :contact        {:name ..name1..
                                       :url  ..url1..
                                       :email ..email1..}
                      :license        {:name ..name2..
                                       :url ..url2..}
                      :x-kikka        ..kikka..}
        :basePath    ..basepath..
        :consumes    ["application/json" "application/edn"]
        :produces    ["application/json" "application/edn"]
        :paths       {"/api/path/{id}" {:get {:tags [:tag1 :tag2 :tag3]
                                              :summary ..summary1..
                                              :description ..description2..
                                              :externalDocs {:url ..url3..
                                                             :description ..description3..}
                                              :operationId  ..operationid..
                                              :consumes     ["application/xyz"]
                                              :produces     ["application/xyz"]
                                              :parameters   [{:in          :path
                                                              :name        "id"
                                                              :description ""
                                                              :required    true
                                                              :type        "integer"
                                                              :format      "int32"}]
                                              :responses    {200 {:description "ok"
                                                                  :schema {:$ref "#/definitions/Pet"}}
                                                             404 {:description "fail"
                                                                  :schema {:$ref "#/definitions/PetError"}}}}}}
        :definitions {:Pet      {:required   [:id :name]
                                 :properties {:id        {:type        "integer"
                                                          :format      "int64"
                                                          :description "Unique identifier for the Pet"
                                                          :minimum     "0.0"
                                                          :maximum     "100.0"}
                                              :name      {:type        "string"
                                                          :description "Friendly name of the pet"}
                                              :category  {:$ref        "#/definitions/Category"
                                                          :description "Category the pet is in"}
                                              :photoUrls {:type        "array"
                                                          :items       {:type "string"}
                                                          :description "Image URLs"}
                                              :tags      {:type        "array"
                                                          :items       {:$ref "#/definitions/Tag"}
                                                          :description "Tags assigned to this pet"}
                                              :status    {:enum        [:pending :sold :available]
                                                          :type        "string"
                                                          :description "pet status in the store"}}}
                      :Category {:properties {:id   {:type        "integer"
                                                     :format      "int64"
                                                     :description "Category unique identifier"
                                                     :minimum     "0.0"
                                                     :maximum     "100.0"}
                                              :name {:type        "string"
                                                     :description "Name of the category"}}}
                      :Tag      {:properties {:id   {:type        "integer"
                                                     :format      "int64"
                                                     :description "Unique identifier for the tag"}
                                              :name {:type        "string"
                                                     :description "Friendly name for the tag"}}}
                      :PetError {:properties {:message {:type "string"}}
                                 :required [:message]}}}))

