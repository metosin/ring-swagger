(ns ring.swagger.common-test
  (:require [midje.sweet :refer :all]
            [ring.swagger.common :refer :all]))

(fact "fn-> & fn->>"
  (let [inc-x  (fn-> :x inc)
        sum-vals (fn->> vals (apply +))
        m {:x 1 :y 2 :z 3}]
    (inc-x m) => 2
    (sum-vals m) => 6))

(fact "remove-empty-keys"
  (remove-empty-keys {:a nil :b false :c 0}) => {:b false :c 0})

(def Abba "jabba")

(fact "name-of"
  (name-of Abba)   => "jabba"
  (name-of 'Abba)  => "Abba"
  (name-of #'Abba) => "Abba"
  (name-of "Abba") => "Abba"
  (name-of :Abba)  => "Abba"
  (name-of {})     => nil
  (name-of nil)    => nil)

(fact "value-of"
  (value-of Abba)   => "jabba"
  (value-of 'Abba)  => "jabba"
  (value-of #'Abba) => "jabba"
  (value-of :abba   => :abba))

(fact "extractors"

  (fact "extract-map"
    (extract-parameters [{:a 1 :b 2}]) => [{} [{:a 1 :b 2}]]
    (extract-parameters [{:a 1 :b 2} ..any..]) => [{:a 1 :b 2} [..any..]])

  (fact "extract-parameters"
    (extract-parameters [:kikka 1 :kakka 2 ..any..]) => [{:kikka 1 :kakka 2} [..any..]]
    (extract-parameters [:kikka 1 :kakka 2 :kukka]) => [{:kikka 1 :kakka 2} [:kukka]]
    (extract-parameters [:kikka 1 :kakka ..any..]) => [{:kikka 1 :kakka ..any..} []])

  (fact "extract-parameters keeps key order"
    (-> (extract-parameters [:a 1 :b 1 :c 1 :d 1 ..any..]) first keys) => [:a :b :c :d]
    (-> (extract-parameters [:b 1 :e 1 :c 1 :a 1 ..any..]) first keys) => [:b :e :c :a])

  (fact "extract none"
    (extract-parameters [..any..]) => [{} [..any..]]))
