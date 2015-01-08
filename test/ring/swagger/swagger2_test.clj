(ns ring.swagger.swagger2-test
  (:require [schema.core :as s]
            [ring.swagger.swagger2 :refer :all]
            [ring.swagger.validator :refer [validate]]
            [cheshire.core :as json]
            [midje.sweet :refer :all]))

(s/defschema LegOfPet {:length Long})

(s/defschema Pet {:id Long
                  :name String
                  :leg LegOfPet
                  (s/optional-key :weight) Double})

(s/defschema Parrot {:name String
                     :type {:name String}})

(s/defschema NotFound {:message s/Str})

(def swagger-with-models
  {:swagger "2.0"
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
                             :parameters {:body Nothing
                                          :query (merge Anything {:x Long :y Long})
                                          :path {:id String}
                                          :header Anything
                                          :formData Anything}
                             :responses {200 {:description "ok"
                                              :schema {:sum Long}}
                                         400 {:description "not found"
                                              :schema NotFound}
                                         :default {:description "error"
                                                   :schema {:code Long}}}}}
           "/api/parrots" {:get {:responses {200 {:schema Parrot
                                                  :description ""}}}}
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
                               :responses {200 {:description "ok"
                                                :schema {:sum Long}}
                                           :default {:description "error"
                                                     :schema {:code Long}}}}
                        :put {:parameters {:body [Pet]
                                           :query (merge Anything {:x Long :y Long})
                                           :path Nothing
                                           :header Anything
                                           :formData Anything}
                              :responses {200 {:description "ok"
                                               :schema {:sum Long}}
                                          :default {:description "error"
                                                    :schema {:code Long}}}}}}})

(fact "is valid"
  (s/check Swagger swagger-with-models) => nil)

(fact "swagger 2.0 spec"
  (validate (json/generate-string (swagger-json swagger-with-models))) => nil)

