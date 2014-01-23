(ns ring.swagger.core-test
  (:require [midje.sweet :refer :all]
            [schema.core :as s]
            [ring.swagger.schema :refer :all]
            [ring.swagger.core :refer :all]))

;;
;; Schema Transformations
;;

(defmodel Tag {(optional :id)   (field s/Int {:description "Unique identifier for the tag"})
               (optional :name) (field s/Str {:description "Friendly name for the tag"})})

(def Tag' {:id "Tag"
           :properties {:id {:type "integer"
                             :format "int64"
                             :description "Unique identifier for the tag"}
                        :name {:type "string"
                               :description "Friendly name for the tag"}}})

(defmodel Category {(optional :id)   (field s/Int {:description "Category unique identifier" :minimum "0.0" :maximum "100.0"})
                    (optional :name) (field s/Str {:description "Name of the category"})})

(def Category' {:id "Category"
                :properties {:id {:type "integer"
                                  :format "int64"
                                  :description "Category unique identifier"
                                  :minimum "0.0"
                                  :maximum "100.0"}
                             :name {:type "string"
                                    :description "Name of the category"}}})

(defmodel Pet  {:id                   (field s/Int {:description "Unique identifier for the Pet" :minimum "0.0" :maximum "100.0"})
                :name                 (field s/Str {:description "Friendly name of the pet"})
                (optional :category)  (field Category {:description "Category the pet is in"})
                (optional :photoUrls) (field [s/Str] {:description "Image URLs"})
                (optional :tags)      (field [Tag] {:description "Tags assigned to this pet"})
                (optional :status)    (field (s/enum :available :pending :sold) {:description "pet status in the store"})})

(def Pet' {:id "Pet"
           :required [:id :name]
           :properties {:id {:type "integer"
                             :format "int64"
                             :description "Unique identifier for the Pet"
                             :minimum "0.0"
                             :maximum "100.0"}
                        :category {:$ref "Category"
                                   :description "Category the pet is in"}
                        :name {:type "string"
                               :description "Friendly name of the pet"}
                        :photoUrls {:type "array"
                                    :description "Image URLs"
                                    :items {:type "string"}}
                        :tags {:type "array"
                               :description "Tags assigned to this pet"
                               :items {:$ref "Tag"}}
                        :status {:type "string"
                                 :description "pet status in the store"
                                 :enum [:pending :sold :available]}}})

(facts "simple schemas"
  (transform 'Tag) => Tag'
  (transform 'Category) => Category'
  (transform 'Pet) => Pet')

(fact "Swagger Petstore example"
  (transform-models 'Pet) => {:Pet Pet'
                              :Tag Tag'
                              :Category Category'})

;;
;; Route generation
;;

(fact "extract-path-parameters"
  (extract-path-parameters "/api/:kikka/:kakka/:kukka") => [:kikka :kakka :kukka])

(fact "swagger-path"
  (swagger-path "/api/:kikka/:kakka/:kukka") => "/api/{kikka}/{kakka}/{kukka}")

(fact "generate-nick"
  (generate-nick (->Route :get "/api/pizzas/:id")) => "getApiPizzasById"
  (generate-nick (->Route :delete "/api/:version/pizzas/:id")) => "deleteApiByVersionPizzasById")

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
      {:apiVersion ..version..}
      ..basepath..
      {}) => (has-body {:swaggerVersion "1.2"
                        :apiVersion ..version..
                        :basePath ..basepath..
                        :resourcePath ""
                        :produces ["application/json"]
                        :models {}
                        :apis []})))

