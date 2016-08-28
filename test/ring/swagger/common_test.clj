(ns ring.swagger.common-test
  (:require [midje.sweet :refer :all]
            [ring.swagger.common :as common]
            [linked.core :as linked]))

(fact "remove-empty-keys"
  (common/remove-empty-keys {:a nil :b false :c 0}) => {:b false :c 0})

(def Abba "jabba")

(fact "value-of"
  (common/value-of Abba) => "jabba"
  (common/value-of 'Abba) => "jabba"
  (common/value-of #'Abba) => "jabba"
  (common/value-of :abba) => :abba)

(fact "extractors"

  (fact "extract-map"
    (common/extract-parameters [{:a 1 :b 2}]) => [{} [{:a 1 :b 2}]]
    (common/extract-parameters [{:a 1 :b 2} ..any..]) => [{:a 1 :b 2} [..any..]])

  (fact "extract-parameters"
    (common/extract-parameters [:kikka 1 :kakka 2 ..any..]) => [{:kikka 1 :kakka 2} [..any..]]
    (common/extract-parameters [:kikka 1 :kakka 2 :kukka]) => [{:kikka 1 :kakka 2} [:kukka]]
    (common/extract-parameters [:kikka 1 :kakka ..any..]) => [{:kikka 1 :kakka ..any..} []])

  (fact "extract-parameters keeps key order"
    (-> (common/extract-parameters [:a 1 :b 1 :c 1 :d 1 ..any..]) first keys) => [:a :b :c :d]
    (-> (common/extract-parameters [:b 1 :e 1 :c 1 :a 1 ..any..]) first keys) => [:b :e :c :a])

  (fact "extract none"
    (common/extract-parameters [..any..]) => [{} [..any..]]))

(defrecord ARecord [x])

(fact "plain-map?"
  (common/plain-map? {}) => true
  (common/plain-map? (->ARecord 1)) => false
  (common/plain-map? (linked/map :a 1)) => true)

(fact "java-invoke"
  (common/java-invoke "java.lang.String" "length" "kikka") => 5)
