(ns ring.swagger.common-test
  (:require [midje.sweet :refer :all]
            [ring.swagger.common :refer :all]))

(fact "path-vals"
  (let [original {:a {:b {:c 1
                          :d 2}
                      :e 3}}
        target [[[:a :e] 3]
                [[:a :b :d] 2]
                [[:a :b :c] 1]]]
    (path-vals original) => target
    (assoc-in-path-vals target) => original))

(defrecord Plane [x y z])
(def *tuples [[:x 1] [:y 2] [:z 3]])
(def *map    {:x 1 :y 2 :z 3})
(def *record (->Plane 1 2 3))

(fact "fn-> & fn->>"
  (let [inc-x  (fn-> :x inc)
        sum-vals (fn->> vals (apply +))]
    (inc-x *map) => 2
    (sum-vals *map) => 6))

(fact "->map"
  (->map *map) => *map
  (->map *tuples) => *map
  (->map *record) => *map)

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

(require '[potemkin :refer [import-vars]])
(import-vars [clojure.walk walk])

(fact "re-resolve"

  (fact "potemkin'd var is imported locally"
    #'walk => #'ring.swagger.common-test/walk)

  (fact "non-symbol/var resolves to nil"
    (re-resolve 1) => nil)

  (fact "re-resolve to the rescue!"
    (re-resolve walk) => #'clojure.walk/walk
    (re-resolve 'walk) => #'clojure.walk/walk
    (re-resolve #'walk) => #'clojure.walk/walk))

(defmacro re-resolve-in-compile-time [sym]
  (let [resolved (re-resolve sym)]
    `~resolved))

(defmacro eval-re-resolve-in-compile-time [sym]
  (let [resolved (eval-re-resolve sym)]
    `~resolved))

(fact "re-resolve in compile-time"
  (fact "re-resolve does not work with macros"
    (re-resolve-in-compile-time 'walk) => nil)
  (fact "eval-re-resolve works with macros"
    (eval-re-resolve-in-compile-time 'walk) => #'clojure.walk/walk))


(fact "extractors"

  (fact "extract-map"
    (extract-parameters [{:a 1 :b 2}]) => [{} [{:a 1 :b 2}]]
    (extract-parameters [{:a 1 :b 2} ..any..]) => [{:a 1 :b 2} [..any..]])

  (fact "extract-parameters"
    (extract-parameters [:kikka 1 :kakka 2 ..any..]) => [{:kikka 1 :kakka 2} [..any..]]
    (extract-parameters [:kikka 1 :kakka 2 :kukka]) => [{:kikka 1 :kakka 2} [:kukka]]
    (extract-parameters [:kikka 1 :kakka ..any..]) => [{:kikka 1 :kakka ..any..} []])

  (fact "extract none"
    (extract-parameters [..any..]) => [{} [..any..]]))
