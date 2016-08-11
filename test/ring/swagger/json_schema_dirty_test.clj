(ns ring.swagger.json-schema-dirty-test
  (:require [midje.sweet :refer :all]
            [schema.core :as s]
            [ring.swagger.json-schema :as rsjs]
            [ring.swagger.json-schema-dirty]
            [schema.experimental.abstract-map :as abstract-map]))

;; Example: https://github.com/swagger-api/swagger-spec/blob/481e4f2aa9b5a73f557ae5aa2155c0b013bd0358/fixtures/v2.0/json/models/modelWithComposition.json#L3

(s/defschema Pet
  (abstract-map/abstract-map-schema :pet-type {:pet-type s/Str
                                               :name s/Str}))

(abstract-map/extend-schema Cat Pet [:cat] {:hunting-skill (s/enum :clueless :lazy :adventurous :aggressive)})
(abstract-map/extend-schema Dog Pet [:dog] {:pack-size s/Int})
(abstract-map/extend-schema Fish Pet [:fish] {:fins s/Int})

(facts "type transformations"
  (rsjs/->swagger Pet) => {:discriminator "pet-type"
                           :properties {:name {:type "string"}
                                        :pet-type {:type "string"}}}

  (rsjs/->swagger Cat) => {:allOf [{:$ref "#/definitions/Pet"}
                                   {:properties {:hunting-skill {:type "string"
                                                                 ; FIXME: Set used to set in same order as what is produced
                                                                 :enum (seq #{:clueless :lazy :adventurous :aggressive})}}}]})
