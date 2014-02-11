(ns ring.swagger.schema-test
  (:require [midje.sweet :refer :all]
            [schema.core :as s]
            [ring.swagger.schema :refer :all]))

(defmodel Inty {:int s/Int})

(fact "models"

  (fact "model?"
    (model? Inty) => true
    (model? 'Inty) => false
    (model? #'Inty) => false
    (model? {:int s/Int}) => false)

  (fact "model-of"
    (model-of Inty) => #'Inty
    (model-of 'Inty) => #'Inty
    (model-of #'Inty) => #'Inty)

  (fact "schema-name"
    (schema-name Inty) => "Inty"
    (schema-name 'Inty) => "Inty"
    (schema-name #'Inty) => "Inty"))

(fact "enum?"
  (enum? s/Int) => false
  (enum? (s/enum [:a :b])) => true)

(def types [Int* Long* Float* Double* Str* Byte* Boolean* Date* DateTime*])

(facts "datatypes can act as fields"
  (doseq [type types]
    (fact {:midje/description (s/explain type)}
      (meta (field type {:a 1})) => {:a 1})))
