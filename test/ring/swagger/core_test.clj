(ns ring.swagger.core-test
  (:require [midje.sweet :refer :all]
            [schema.core :as s]
            [ring.swagger.test-utils :refer :all]
            [ring.swagger.schema :refer :all]
            [ring.swagger.core :refer :all]
            [linked.core :as linked]))

(s/defschema OrderedSchema (linked/map
                             :id Long
                             :hot Boolean
                             :tag (s/enum :kikka :kukka)
                             :chief [{:name String
                                      :type #{{:id String}}}]
                             :a String
                             :b String
                             :c String
                             :d String
                             :e String
                             :f String))

(def ordered-schema-order (keys OrderedSchema))

(s/defschema RootModel
  {:sub {:foo Long}})

(def AnonymousRootModel
  {:sub {:foo Long}})

(fact "name-schemas"
  (fact "Adds name to basic sub-schema"
    (meta (:sub (name-schemas ['Root] RootModel)))
    => {:name 'RootSub})
  (fact "Works with deep schemas"
    (meta (:sub2 (:sub1 (name-schemas ['Root] {:sub1 {:sub2 {:foo Long}}}))))
    => {:name 'RootSub1Sub2})
  (fact "Adds names to maps inside Schemas"
    (meta (:schema (:sub (name-schemas ['Root] {:sub (s/maybe {:a s/Str})}))))
    => {:name 'RootSub})
  (fact "Uses name of optional-key"
    (meta (get (name-schemas ['Root] {(s/optional-key :sub) {:a s/Str}}) (s/optional-key :sub)))
    => {:name 'RootSub})
  (fact "generated names for non-spesific keys"
    (str (:name (meta (get (name-schemas ["Root"] {s/Keyword {:a s/Str}}) s/Keyword))))
    => #"^RootKeyword\d+")

  (fact "empty vector is not map-entry"
    (name-schemas ["Root"] [[]])
    => [[]]))

(fact "with-named-sub-schemas"
  (fact "add :name meta-data to sub-schemas"
    (meta (:sub (with-named-sub-schemas RootModel))) => {:name 'RootModelSub})

  (fact "add :name meta-data to anonymous sub-schemas"
    (meta (:sub (with-named-sub-schemas AnonymousRootModel))) => (contains {:name #(.startsWith (str %) "Schema")}))

  (fact "add prefixed :name meta-data to anonymous sub-schemas"
    (meta (:sub (with-named-sub-schemas AnonymousRootModel "Body"))) => (contains {:name #(.startsWith (str %) "Body")}))

  (fact "Keeps the order"
    (keys (with-named-sub-schemas OrderedSchema)) => ordered-schema-order)

  (fact "Retains metadata, fixes #https://github.com/metosin/compojure-api/issues/168"
    (meta
      (with-named-sub-schemas
        (with-meta
          {:a Long}
          {:kikka :kukka})
        "Body")) => (contains {:kikka :kukka})))

;;
;; Route generation
;;

(tabular
  (fact path-params
    (path-params ?input) => ?output)
  ?input                        ?output
  "/api/:kikka/:kakka/:kukka"   [:kikka :kakka :kukka]
  "/api/:kikka/kakka/:kukka"    [:kikka :kukka]
  "/:foo-bar/:foo_bar"          [:foo-bar :foo_bar]
  "/api/ping"                   empty?)

;;
;; Common
;;

(fact "swagger-path"
  (swagger-path "/api/:kikka/:kakka/ko?/:kukka/kokka") => "/api/{kikka}/{kakka}/ko?/{kukka}/kokka")

(fact "scrict-schema strips open keys"
  (strict-schema {s/Keyword s/Any :s String}) => {:s String})

(fact join-paths
  (join-paths "/foo" nil "index.html") => "/foo/index.html"
  (join-paths nil "/foo/" "index.html") => "/foo/index.html"
  (join-paths nil "" "/foo" "" "" "index.html") => "/foo/index.html"
  (join-paths "/foo" "") => "/foo")

(fact "(servlet-)context"
  (context {}) => ""
  (context {:servlet-context (fake-servlet-context "/kikka")}) => "/kikka")

(fact "basepath"
  (fact "http"
    (basepath {:scheme "http" :server-name "www.metosin.fi" :server-port 80 :headers {}}) => "http://www.metosin.fi"
    (basepath {:scheme "http" :server-name "www.metosin.fi" :server-port 8080 :headers {}}) => "http://www.metosin.fi:8080")
  (fact "https"
    (basepath {:scheme "https" :server-name "www.metosin.fi" :server-port 443 :headers {}}) => "https://www.metosin.fi"
    (basepath {:scheme "https" :server-name "www.metosin.fi" :server-port 8443 :headers {}}) => "https://www.metosin.fi:8443")
  (fact "with servlet-context"
    (basepath {:scheme "http" :server-name "www.metosin.fi" :server-port 80 :headers {} :servlet-context (fake-servlet-context "/kikka")}) => "http://www.metosin.fi/kikka")
  (fact "x-forwarded-proto"
    (fact "we trust the given 'x-forwarded-proto'"
      (basepath {:scheme "http" :server-name "www.metosin.fi" :server-port 443 :headers {"x-forwarded-proto" "https"}}) => "https://www.metosin.fi")
    (fact "can't fake the protocol from https to http"
      (basepath {:scheme "https" :server-name "www.metosin.fi" :server-port 443 :headers {"x-forwarded-proto" "http"}}) => "https://www.metosin.fi")))
