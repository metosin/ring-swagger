(ns ring.swagger.json-schema-dirty-test
  (:require [midje.sweet :refer :all]
            [schema.core :as s]
            [ring.swagger.json-schema :refer :all]
            [ring.swagger.json-schema-dirty :refer :all]))

(facts "type transformations"
  (fact "schema predicates"
    (->json (s/conditional (constantly true) Long (constantly false) String))
    => {:type "void" :oneOf [(->json Long) (->json String)]})

  (fact "s/if"
    (->json (s/if (constantly true) Long String))
    => {:type "void" :oneOf [(->json Long) (->json String)]})

  (fact "s/either"
    (->json (s/either Long String))
    => {:type "void" :oneOf [(->json Long) (->json String)]}))
