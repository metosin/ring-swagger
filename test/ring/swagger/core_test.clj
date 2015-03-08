(ns ring.swagger.core-test
  (:require [midje.sweet :refer :all]
            [schema.core :as s]
            [ring.swagger.test-utils :refer :all]
            [ring.swagger.schema :refer :all]
            [ring.swagger.core :refer :all]
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

(def Tag' {:id 'Tag
           :properties {:id {:type "integer"
                             :format "int64"
                             :description "Unique identifier for the tag"}
                        :name {:type "string"
                               :description "Friendly name for the tag"}}})

(def Category' {:id 'Category
                :properties {:id {:type "integer"
                                  :format "int64"
                                  :description "Category unique identifier"
                                  :minimum "0.0"
                                  :maximum "100.0"}
                             :name {:type "string"
                                    :description "Name of the category"}}})

(def Pet' {:id 'Pet
           :required [:id :name]
           :properties {:id {:type "integer"
                             :format "int64"
                             :description "Unique identifier for the Pet"
                             :minimum "0.0"
                             :maximum "100.0"}
                        :category {:$ref 'Category
                                   :description "Category the pet is in"}
                        :name {:type "string"
                               :description "Friendly name of the pet"}
                        :photoUrls {:type "array"
                                    :description "Image URLs"
                                    :items {:type "string"}}
                        :tags {:type "array"
                               :description "Tags assigned to this pet"
                               :items {:$ref 'Tag}}
                        :status {:type "string"
                                 :description "pet status in the store"
                                 :enum [:pending :sold :available]}}})

(def PetError' {:id 'PetError
                :required [:message]
                :properties {:message {:type "string"}}})
;;
;; Facts
;;

(facts "simple schemas"
  (transform Tag) => Tag'
  (transform Category) => Category'
  (transform Pet) => Pet')

(s/defschema RootModel
  {:sub {:foo Long}})

(fact "name-schemas"
  (fact "Adds name to basic sub-schema"
    (meta (:sub (name-schemas ['Root] RootModel)))
    => {:name 'RootSub})
  (fact "Works with deep schemas"
    (meta (:sub2 (:sub1 (name-schemas ['Root] {:sub1 {:sub2 {:foo Long}}}))))
    => {:name 'RootSub1Sub2})
  (fact "Adds names to maps inside Schemas"
    (meta (:schema (:sub (name-schemas ['Root] {:sub (s/maybe {:a s/Str})}))))
    => {:name 'RootSub})
  (fact "Uses name of optional-key"
    (meta (get (name-schemas ['Root] {(s/optional-key :sub) {:a s/Str}}) (s/optional-key :sub)))
    => {:name 'RootSub}))

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

  (fact "Inline-sub-schemas as collected after they are nameed"
    (collect-models (with-named-sub-schemas RootModel))
    => {'RootModel RootModel
        'RootModelSub (:sub RootModel)})

  (fact "Described anonymous models are collected"
    (let [schema (describe {:sub (describe {:foo Long} "the sub schema")} "the root schema")]
      (keys (collect-models (with-named-sub-schemas schema))) => (two-of symbol?))))

(fact "transform-models"
  (transform-models [Pet]) => {'Pet Pet'
                               'Tag Tag'
                               'Category Category'}

  (s/defschema Foo (s/enum :a :b))
  (s/defschema Bar {:key Foo})
  (s/defschema Baz s/Keyword)

  (fact "record-schemas are not transformed"
    (transform-models [Foo]) => {})

  (fact "non-map schemas are not transformed"
    (transform-models [Baz]) => {})

  (fact "nested record-schemas are inlined"
    (transform-models [Bar]) => {'Bar {:id 'Bar,
                                       :properties {:key {:enum [:b :a]
                                                          :type "string"}}
                                       :required [:key]}})

  (fact "nested schemas"

    (fact "with anonymous sub-schemas"
      (s/defschema Nested {:id s/Str
                           :address {:country (s/enum :fi :pl)
                                     :street {:name s/Str}}})
      (transform-models [(with-named-sub-schemas Nested)])

      =>

      {'Nested {:id 'Nested
                :properties {:address {:$ref 'NestedAddress}
                             :id {:type "string"}}
                :required [:id :address]}
       'NestedAddress {:id 'NestedAddress
                       :properties {:country {:enum [:fi :pl]
                                              :type "string"}
                                    :street {:$ref 'NestedAddressStreet}}
                       :required [:country :street]}
       'NestedAddressStreet {:id 'NestedAddressStreet
                             :properties {:name {:type "string"}}
                             :required [:name]}})

    (fact "nested named sub-schemas"

      (s/defschema Boundary
        {:type (s/enum "MultiPolygon" "Polygon" "MultiPoint" "Point")
         :coordinates [s/Any]})

      (s/defschema ReturnValue
        {:boundary (s/maybe Boundary)})

      (keys
        (transform-models
          [(with-named-sub-schemas ReturnValue)])) => ['Boundary 'ReturnValue])))

;;
;; Route generation
;;

(tabular
  (fact path-params
    (path-params ?input) => ?output)
  ?input                        ?output
  "/api/:kikka/:kakka/:kukka"   [:kikka :kakka :kukka]
  "/api/:kikka/kakka/:kukka"    [:kikka :kukka]
  "/:foo-bar/:foo_bar"          [:foo-bar :foo_bar]
  "/api/ping"                   empty?)

(fact "string-path-parameters"
  (string-path-parameters "/api/:kikka/:kakka/:kukka") => {:type :path
                                                           :model {:kukka java.lang.String
                                                                   :kakka java.lang.String
                                                                   :kikka java.lang.String}}
  (string-path-parameters "/api/ping") => nil)

(s/defschema Query {:id Long (s/optional-key :q) String})
(s/defschema Body {:name String :age Long})
(s/defschema Path {:p Long})

(fact "convert-parameters"

  (fact "all parameter types can be converted"
    (convert-parameters
      [{:model Query
        :type :query}
       {:model Body
        :type :body}
       {:model Path
        :type :path}]) => [{:name "id"
                            :description ""
                            :format "int64"
                            :paramType :query
                            :required true
                            :type "integer"}
                           {:name "q"
                            :description ""
                            :paramType :query
                            :required false
                            :type "string"}
                           {:name "body"
                            :description ""
                            :paramType :body
                            :required true
                            :type 'Body}
                           {:name "p"
                            :description ""
                            :format "int64"
                            :paramType :path
                            :required true
                            :type "integer"}])

  (fact "anonymous schemas can be used with ..."

    (doseq [type [:query :path]]
      (fact {:midje/description (str "... " type "-parameters")}

        (convert-parameters
          [{:type type
            :model {s/Keyword s/Any
                    :q String
                    (s/optional-key :l) Long}}])

        => [{:name "q"
             :description ""
             :paramType type
             :required true
             :type "string"}
            {:name "l"
             :description ""
             :format "int64"
             :paramType type
             :required false
             :type "integer"}])))

  (fact "Array body parameters"
    (convert-parameters
      [{:type :body
        :model [Body]}])

    => [{:name "body"
         :description ""
         :paramType :body
         :required true
         :items {:$ref 'Body}
         :type "array"}])

  (fact "Body param with desc"
    (convert-parameters [{:type :body
                          :model (describe Body "foo")}])
    => [{:description "foo" :name "body" :paramType :body :required true :type 'Body}]))

;;
;; Helpers
;;

(fact "swagger-path"
  (swagger-path "/api/:kikka/:kakka/:kukka") => "/api/{kikka}/{kakka}/{kukka}")

(fact "generate-nick"
  (generate-nick {:method :get
                  :uri "/api/pizzas/:id"
                  :metadata ..meta..}) => "getApiPizzasById"
  (generate-nick {:method :delete
                  :uri "/api/:version/pizzas/:id"
                  :metadata ..meta..}) => "deleteApiByVersionPizzasById")

(fact "extract-models"
  (fact "returns both return and body-parameters but not query or path parameter types"
    (extract-models {:routes [{:metadata {:return [Tag]
                                          :parameters [{:model Tag
                                                        :type :body}
                                                       {:model [Category]
                                                        :type :body}
                                                       {:model Pet
                                                        :type :path}
                                                       {:model Pet
                                                        :type :query}]}}
                              {:metadata {:return Tag}}]})
    => {'Category Category
        'Tag Tag}))


(declare Bar)

(s/defschema Foo {:bar (s/recursive #'Bar)})

(s/defschema Bar {:foo (s/maybe #'Foo)})

(fact "recursive"
  (collect-models [Foo Bar])
  => {'Bar {:foo (s/maybe #'Foo)}
      'Foo {:bar (s/recursive #'Bar)}}

  (transform-models [Foo Bar])
  => {'Bar {:id 'Bar
            :properties {:foo {:$ref 'Foo}}
            :required [:foo]}
      'Foo {:id 'Foo
            :properties {:bar {:$ref 'Bar}}
            :required [:bar]}})

(fact "with-named-sub-schemas"
  (fact "nested maps"
    (transform (with-named-sub-schemas  {:a String
                                         :b {:c String}})) => truthy)
  (fact "nested vectors"
    (transform (with-named-sub-schemas {:a String
                                        :b [{:c String}]})) => truthy)
  (fact "nested sets"
    (transform (with-named-sub-schemas {:a String
                                        :b #{{:c String}}})) => truthy)

  ;; FIXME: should work
  (fact "nested value behind a record"
    (transform
      (with-named-sub-schemas
        {:a String
         :b (s/maybe {:c String})})) => truthy))

;;
;; Final json
;;

(facts "api-listing"
  (fact "without parameters"
    (:body (api-listing {} {})) => {:swaggerVersion "1.2"
                                    :apiVersion "0.0.1"
                                    :apis []
                                    :info {}
                                    :authorizations {}})

  (fact "with parameters"
    (:body (api-listing {:apiVersion ...version...
                         :title ..title..
                         :description ..description..
                         :termsOfServiceUrl ..terms..
                         :contact ..contact..
                         :license ..license..
                         :licenseUrl ..licenseUrl..
                         :authorizations {:not :validated}} {}))

    => {:swaggerVersion "1.2"
        :apiVersion ...version...
        :info {:title ..title..
               :description ..description..
               :termsOfServiceUrl ..terms..
               :contact ..contact..
               :license ..license..
               :licenseUrl ..licenseUrl..}
        :apis []
        :authorizations {:not :validated}})

  (fact "apis"
    (fact "none"
      (-> (api-listing ..map.. {}) :body :apis) => [])
    (fact "some"
      (-> (api-listing ..map.. {"api1" {}
                                "api2" {:description ..desc..}})
          :body :apis)
      => [{:path "/api1"
           :description ""}
          {:path "/api2"
           :description ..desc..}])))
(fact "api-declaration"
  (fact "empty api"
    (:body (api-declaration
             {}
             {..api.. {}}
             ..api..
             ..basepath..))
    => {:swaggerVersion "1.2"
        :apiVersion "0.0.1"
        :basePath ..basepath..
        :resourcePath "/"
        :produces ["application/json"]
        :consumes ["application/json"]
        :models {}
        :apis []})
  (fact "more full api"
    (s/defschema Q {:q String})
    (:body
      (api-declaration
        {:apiVersion ..version..
         :produces ["application/json"
                    "application/xml"]
         :consumes ["application/json"
                    "application/xml"]}
        {..api.. {:routes [{:method :get
                            :uri "/pets/:id"
                            :metadata {:return Pet
                                       :summary ..summary..
                                       :notes ..notes..
                                       :responseMessages [{:code 200
                                                           :message "the Pet"
                                                           :responseModel Pet}
                                                          {:code 404
                                                           :message "pet not found"
                                                           :responseModel PetError}]
                                       :parameters [(string-path-parameters "/pets/:id")]}}
                           {:method :get
                            :uri "/pets"
                            :metadata {:return [Pet]
                                       :summary ..summary2..
                                       :notes ..notes2..
                                       :parameters [{:model Q
                                                     :type :query}]}}]}}
        ..api..
        ..basepath..))

    => {:swaggerVersion "1.2"
        :apiVersion ..version..
        :basePath ..basepath..
        :resourcePath "/"
        :produces ["application/json"
                   "application/xml"]
        :consumes ["application/json"
                   "application/xml"]
        :models {'Pet Pet'
                 'Tag Tag'
                 'Category Category'
                 'PetError PetError'}
        :apis [{:operations [{:method "GET"
                              :nickname "getPetsById"
                              :notes ..notes..
                              :responseMessages [{:code 200
                                                  :message "the Pet"
                                                  :responseModel 'Pet}
                                                 {:code 404
                                                  :message "pet not found"
                                                  :responseModel 'PetError}]
                              :parameters [{:description ""
                                            :name "id"
                                            :paramType :path
                                            :required true
                                            :type "string"}]
                              :authorizations {}
                              :summary ..summary..
                              :type 'Pet}]
                :path "/pets/{id}"}
               {:operations [{:method "GET"
                              :nickname "getPets"
                              :notes ..notes2..
                              :responseMessages []
                              :parameters [{:description ""
                                            :name "q"
                                            :paramType :query
                                            :required true
                                            :type "string"}]
                              :authorizations {}
                              :summary ..summary2..
                              :type "array"
                              :items {:$ref 'Pet}}]
                :path "/pets"}]})

  (fact "primitive responses"
    (:body
      (api-declaration
        {}
        {..api.. {:routes [{:method :get
                            :uri "/primitive"
                            :metadata {:return String}}
                           {:method :get
                            :uri "/primitiveArray"
                            :metadata {:return [String]}}]}}
        ..api..
        ..basepath..))

    => {:swaggerVersion "1.2"
        :apiVersion "0.0.1"
        :basePath ..basepath..
        :resourcePath "/"
        :produces ["application/json"]
        :consumes ["application/json"]
        :models {}
        :apis [{:operations [{:method "GET"
                              :nickname "getPrimitive"
                              :notes ""
                              :parameters []
                              :responseMessages []
                              :authorizations {}
                              :summary ""
                              :type "string"}]
                :path "/primitive"}
               {:operations [{:method "GET"
                              :nickname "getPrimitiveArray"
                              :notes ""
                              :parameters []
                              :responseMessages []
                              :authorizations {}
                              :summary ""
                              :type "array"
                              :items {:type "string"}}]
                :path "/primitiveArray"}]}))

;;
;; Web stuff
;;

(fact join-paths
  (join-paths "/foo" nil "index.html") => "/foo/index.html"
  (join-paths nil "/foo/" "index.html") => "/foo/index.html"
  (join-paths nil "" "/foo" "" "" "index.html") => "/foo/index.html"
  (join-paths "/foo" "") => "/foo")

(fact "(servlet-)context"
  (context {}) => ""
  (context {:servlet-context (fake-servlet-context "/kikka")}) => "/kikka")

(fact "basepath"
  (fact "http"
    (basepath {:scheme "http" :server-name "www.metosin.fi" :server-port 80 :headers {}}) => "http://www.metosin.fi"
    (basepath {:scheme "http" :server-name "www.metosin.fi" :server-port 8080 :headers {}}) => "http://www.metosin.fi:8080")
  (fact "https"
    (basepath {:scheme "https" :server-name "www.metosin.fi" :server-port 443 :headers {}}) => "https://www.metosin.fi"
    (basepath {:scheme "https" :server-name "www.metosin.fi" :server-port 8443 :headers {}}) => "https://www.metosin.fi:8443")
  (fact "with servlet-context"
    (basepath {:scheme "http" :server-name "www.metosin.fi" :server-port 80 :headers {} :servlet-context (fake-servlet-context "/kikka")}) => "http://www.metosin.fi/kikka")
  (fact "x-forwarded-proto"
    (fact "we trust the given 'x-forwarded-proto'"
      (basepath {:scheme "http" :server-name "www.metosin.fi" :server-port 443 :headers {"x-forwarded-proto" "https"}}) => "https://www.metosin.fi")
    (fact "can't fake the protocol from https to http"
      (basepath {:scheme "https" :server-name "www.metosin.fi" :server-port 443 :headers {"x-forwarded-proto" "http"}}) => "https://www.metosin.fi")))
