(ns ring.swagger.core-test
  (:require [midje.sweet :refer :all]
            [schema.core :as s]
            [ring.swagger.test-utils :refer :all]
            [ring.swagger.schema :refer :all]
            [ring.swagger.core :refer :all]
            [flatland.ordered.map :refer :all]))

;;
;; Helpers
;;

(fact "scrict-schema strips open keys"
  (strict-schema {s/Keyword s/Any :s String}) => {:s String})

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

(def AnonymousRootModel
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
    => {:name 'RootSub})
  (fact "generated names for non-spesific keys"
    (str (:name (meta (get (name-schemas ["Root"] {s/Keyword {:a s/Str}}) s/Keyword))))
    => #"^RootKeyword\d+"))

(fact "with-named-sub-schemas"
  (fact "add :name meta-data to sub-schemas"
    (meta (:sub (with-named-sub-schemas RootModel))) => {:name 'RootModelSub})

  (fact "add :name meta-data to anonymous sub-schemas"
    (meta (:sub (with-named-sub-schemas AnonymousRootModel))) => (contains {:name #(.startsWith (str %) "Schema")}))

  (fact "add prefixed :name meta-data to anonymous sub-schemas"
    (meta (:sub (with-named-sub-schemas AnonymousRootModel "Body"))) => (contains {:name #(.startsWith (str %) "Body")}))

  (fact "Keeps the order"
    (keys (with-named-sub-schemas OrderedSchema)) => ordered-schema-order))

(fact "collect-models"
  (fact "Sub-schemas are collected"
    (collect-models Pet)
    => {'Pet #{Pet}
        'Tag #{Tag}
        'Category #{Category}})

  (fact "No schemas are collected if all are unnamed"
    (collect-models String) => {})

  (fact "Inline-sub-schemas as collected after they are nameed"
    (collect-models (with-named-sub-schemas RootModel))
    => {'RootModel #{RootModel}
        'RootModelSub #{(:sub RootModel)}})

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
          [(with-named-sub-schemas ReturnValue)])) => (just ['Boundary 'ReturnValue] :in-any-order))))

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
  => {'Bar #{{:foo (s/maybe #'Foo)}}
      'Foo #{{:bar (s/recursive #'Bar)}}}

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
