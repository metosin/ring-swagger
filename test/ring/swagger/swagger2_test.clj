(ns ring.swagger.swagger2-test
  (:require [schema.core :as s]
            [ring.swagger.core2 :refer :all]
            [ring.swagger.spec :as spec]
            [midje.sweet :refer :all]
            [clojure.pprint :refer [pprint]]))

(s/defschema LegOfPet {:length Long})

(s/defschema Pet {:id Long
                  :name String
                  :leg LegOfPet
                  (s/optional-key :weight) Double})

(s/defschema Parrot {:name String
                     :type {:name String}})

(s/defschema NotFound {:message s/Str})

(def swagger-with-models
  {:swagger 2.0
   :info {:version "version"
          :title "title"
          :description "description"
          :termsOfService "jeah"
          :contact {:name "name"
                    :url "url"
                    :email "email"}
          :licence {:name "name"
                    :url "url"}
          :x-kikka "jeah"}
   :basePath "/"
   :consumes ["application/json" "application/edn"]
   :produces ["application/json" "application/edn"]
   :paths {"/api/:id" [{:method :get
                        :tags [:tag1 :tag2 :tag3]
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
                                              :schema {:code Long}}}}]
           "/api/parrots" [{:method :get
                            :parameters {:body Nothing
                                         :query Anything
                                         :path Nothing
                                         :header Anything
                                         :formData Anything}
                            :responses {200 {:schema Parrot
                                             :description ""}}}
                           ]
           "/api/pets" [{:method :get
                         :parameters {:body Pet
                                      :query (merge Anything {:x Long :y Long})
                                      :path Nothing
                                      :header Anything
                                      :formData Anything}
                         :responses {200 {:description "ok"
                                          :schema {:sum Long}}
                                     :default {:description "error"
                                               :schema {:code Long}}}}
                        {:method :post
                         :parameters {:body #{Pet}
                                      :query (merge Anything {:x Long :y Long})
                                      :path Nothing
                                      :header Anything
                                      :formData Anything}
                         :responses {200 {:description "ok"
                                          :schema {:sum Long}}
                                     :default {:description "error"
                                               :schema {:code Long}}}}
                        {:method :put
                         :parameters {:body [Pet]
                                      :query (merge Anything {:x Long :y Long})
                                      :path Nothing
                                      :header Anything
                                      :formData Anything}
                         :responses {200 {:description "ok"
                                          :schema {:sum Long}}
                                     :default {:description "error"
                                               :schema {:code Long}}}}]}})

#_(clojure.pprint/pprint (swagger-json swagger-with-models))

(fact "swagger 2.0 spec"
  (s/check spec/Swagger (swagger-json swagger-with-models)) => nil)
