(ns ring.swagger.json-schema-dirty-test
  (:require [midje.sweet :refer :all]
            [schema.core :as s]
            [ring.swagger.json-schema :refer :all]
            [ring.swagger.json-schema-dirty :refer :all]))

(facts "type transformations"
  (fact "schema predicates"
    (->swagger (s/conditional (constantly true) Long (constantly false) String))
    => {:type "void" :oneOf [(->swagger Long) (->swagger String)]})

  (fact "s/if"
    (->swagger (s/if (constantly true) Long String))
    => {:type "void" :oneOf [(->swagger Long) (->swagger String)]}))
