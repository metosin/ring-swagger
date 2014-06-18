(ns ring.swagger.core-test
  (:require [midje.sweet :refer :all]
            [schema.core :as s]
            [ring.swagger.test-utils :refer :all]
            [ring.swagger.schema :refer :all]
            [ring.swagger.data :refer :all]
            [ring.swagger.core :refer :all])
  (:import  [java.util Date]
            [org.joda.time DateTime LocalDate]))

(defmodel Model {:value String})

(facts "type transformations"

  (facts "java types"
    (->json Long) => {:type "integer" :format "int64"}
    (->json Double) => {:type "number" :format "double"}
    (->json String) => {:type "string"}
    (->json Boolean) => {:type "boolean"}
    (->json Date) => {:type "string" :format "date-time"}
    (->json DateTime) => {:type "string" :format "date-time"}
    (->json LocalDate) => {:type "string" :format "date"})

  (facts "datatypes"
    (->json Long*) => {:type "integer" :format "int64"}
    (->json Double*) => {:type "number" :format "double"}
    (->json String*) => {:type "string"}
    (->json Boolean*) => {:type "boolean"}
    (->json DateTime*) => {:type "string" :format "date-time"}
    (->json Date*) => {:type "string" :format "date"})

  (fact "schema types"
    (->json s/Int) => {:type "integer" :format "int64"}
    (->json s/Str) => {:type "string"})

  (fact "containers"
    (->json [Long]) => {:type "array" :items {:format "int64" :type "integer"}}
    (->json #{Long}) => {:type "array" :items {:format "int64" :type "integer"} :uniqueItems true})

  (facts "nil"
    (->json nil) => {:type "void"})

  (fact "special predicates"

    (fact "s/enum"
      (->json (s/enum :kikka :kakka)) => {:type "string" :enum [:kikka :kakka]}
      (->json (s/enum 1 2 3)) => {:type "integer" :format "int64" :enum (seq #{1 2 3})})

    (fact "s/maybe -> type of internal schema"
      (->json (s/maybe Long)) => (->json Long))

    (fact "s/both -> type of the first element"
      (->json (s/both Long String)) => (->json Long))

    (fact "s/recursive -> type of internal schema"
      (->json (s/recursive #'Model)) => (->json #'Model))

    (fact "s/eq -> type of class of value"
      (->json (s/eq "kikka")) => (->json String))

    (fact "top level ->json"
      (fact "for named schema"
        (->json Pet :top true) => {:type 'Pet})
      (fact "for non-named schema"
        (->json s/Any :top true) => {:type "void"}))))

;;
;; Schemas
;;

(defmodel Tag {(s/optional-key :id)   (field s/Int {:description "Unique identifier for the tag"})
               (s/optional-key :name) (field s/Str {:description "Friendly name for the tag"})})

(defmodel Category {(s/optional-key :id)   (field s/Int {:description "Category unique identifier" :minimum "0.0" :maximum "100.0"})
                    (s/optional-key :name) (field s/Str {:description "Name of the category"})})

(defmodel Pet  {:id                         (field s/Int {:description "Unique identifier for the Pet" :minimum "0.0" :maximum "100.0"})
                :name                       (field s/Str {:description "Friendly name of the pet"})
                (s/optional-key :category)  (field Category {:description "Category the pet is in"})
                (s/optional-key :photoUrls) (field [s/Str] {:description "Image URLs"})
                (s/optional-key :tags)      (field [Tag] {:description "Tags assigned to this pet"})
                (s/optional-key :status)    (field (s/enum :available :pending :sold) {:description "pet status in the store"})})

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

;;
;; Facts
;;

(facts "simple schemas"
  (transform Tag) => Tag'
  (transform Category) => Category'
  (transform Pet) => Pet')

(fact "collect-models"
  (collect-models Pet) => {'Pet Pet
                           'Tag Tag
                           'Category Category}
  (collect-models String) => {})

(fact "transform-models"
  (transform-models [Pet]) => {'Pet Pet'
                               'Tag Tag'
                               'Category Category'})

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

(defmodel Query {:id Long (s/optional-key :q) String})
(defmodel Body {:name String :age Long})
(defmodel Path {:p Long})

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
                    :q String (s/optional-key :l) Long}}])

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
         :type "array"}]))

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
                              {:metadata {:return Tag}}]}) => {'Category Category
                                                               'Tag Tag}))

(facts "generating return types from models, list & set of models"
  (fact "returning Tag"
    (->json Tag :top true) => {:type 'Tag})
  (fact "returning [Tag]"
    (->json [Tag] :top true) => {:items {:$ref 'Tag}, :type "array"})
  (fact "returning #{Tag}"
    (->json #{Tag} :top true) => {:items {:$ref 'Tag}, :type "array" :uniqueItems true}))

(facts "properties"
  (fact "s/Any -values are ignored"
    (keys (properties {:a String
                       :b s/Any})) => [:a]
  (fact "s/Keyword -keys are ignored"
    (keys (properties {:a String
                       s/Keyword s/Any})) => [:a])))

;;
;; Final json
;;

(defn has-body [expected] (chatty-checker [x] (= (-> x :body) expected)))
(defn has-apis [expected] (chatty-checker [x] (= (-> x :body :apis) expected)))

(facts "api-listing"
  (fact "without parameters"
    (api-listing {} {}) => (has-body
                             {:swaggerVersion "1.2"
                              :apiVersion "0.0.1"
                              :apis []
                              :info {}}))
  (fact "with parameters"
    (api-listing {:apiVersion ...version...
                  :title ..title..
                  :description ..description..
                  :termsOfServiceUrl ..terms..
                  :contact ..contact..
                  :license ..licence..
                  :licenseUrl ..licenceUrl..} {}) => (has-body
                                                       {:swaggerVersion "1.2"
                                                        :apiVersion ...version...
                                                        :info {:title ..title..
                                                               :description ..description..
                                                               :termsOfServiceUrl ..terms..
                                                               :contact ..contact..
                                                               :license ..licence..
                                                               :licenseUrl ..licenceUrl..}
                                                        :apis []}))
  (fact "apis"
    (fact "none"
      (api-listing ..map.. {}) => (has-apis []))
    (fact "some"
      (api-listing ..map.. {"api1" {}
                            "api2" {:description ..desc..}}) => (has-apis [{:path "/api1"
                                                                            :description ""}
                                                                           {:path "/api2"
                                                                            :description ..desc..}]))))

(fact "api-declaration"
  (fact "empty api"
    (api-declaration
      {}
      {..api.. {}}
      ..api..
      ..basepath..) => (has-body {:swaggerVersion "1.2"
                                  :apiVersion "0.0.1"
                                  :basePath ..basepath..
                                  :resourcePath ""
                                  :produces ["application/json"]
                                  :consumes ["application/json"]
                                  :models {}
                                  :apis []}))
  (fact "more full api"
    (defmodel Q {:q String})
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
                                     :parameters [(string-path-parameters "/pets/:id")]}}
                         {:method :get
                          :uri "/pets"
                          :metadata {:return [Pet]
                                     :summary ..summary2..
                                     :notes ..notes2..
                                     :parameters [{:model Q
                                                   :type :query}]}}]}}
        ..api..
        ..basepath..)

    => (has-body
         {:swaggerVersion "1.2"
          :apiVersion ..version..
          :basePath ..basepath..
          :resourcePath ""
          :produces ["application/json"
                     "application/xml"]
          :consumes ["application/json"
                     "application/xml"]
          :models {'Pet Pet'
                   'Tag Tag'
                   'Category Category'}
          :apis [{:operations [{:method "GET"
                                :nickname "getPetsById"
                                :notes ..notes..
                                :responseMessages []
                                :parameters [{:description ""
                                              :name "id"
                                              :paramType :path
                                              :required true
                                              :type "string"}]
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
                                :summary ..summary2..
                                :type "array"
                                :items {:$ref 'Pet}}]
                  :path "/pets"}]})))

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
