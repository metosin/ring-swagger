(ns ring.swagger.handler-test
  (:require [midje.sweet :refer :all]
            [ring.swagger.handler :refer :all]
            [plumbing.core :refer [fnk]]
            [ring.util.http-response :refer [ok]]
            [schema.core :as s]))

(fact "fn-handler"
  (let [endpoint (handler identity)]

    (endpoint ..request..) => ..request..
    (meta endpoint) => {}))

(fact "fn-handler with metadata"
  (let [metadata {:tags [:programmer]
                  :summary "programming endpoint"
                  :description "no gardening here"
                  :externalDocs {:url "http://www.metosin.fi"
                                 :description "link to it"}
                  :parameters {:query {:language s/Str, :level s/Int}
                               :path {:id s/Str}}
                  :responses {200 {:description "ok"}
                              :default {:description "error"
                                        :schema {:code Long}}}}
        endpoint (handler metadata identity)]

    (endpoint ..request..) => ..request..
    (meta endpoint) => metadata))

(fact "fnk handler"
  (let [endpoint (handler
                   (fnk
                     [[:request
                       [:query-params x :- s/Int, y :- s/Int]]]
                     (ok [x y])))]

    (endpoint {:request {:query-params {:x 1 :y 2}}}) => (contains {:body [1 2]})
    (meta endpoint) => {:parameters {:query {:x s/Int, :y s/Int, s/Keyword s/Any}}}))

(fact "fnk-handler with metadata"
  (let [endpoint (handler
                   {:tags [:programmer]
                    :responses {200 {:schema [s/Int]}}}
                   (fnk
                     [[:request
                       [:query-params x :- s/Int, y :- s/Int]
                       [:path-params z :- s/Int]]]
                     (ok [x y z])))]

    (endpoint {:request {:query-params {:x 1 :y 2}
                         :path-params {:z 3}}}) => (contains {:body [1 2 3]})
    (meta endpoint) => {:tags [:programmer]
                        :parameters {:query {:x s/Int, :y s/Int, s/Keyword s/Any}
                                     :path {:z s/Int, s/Keyword s/Any}}
                        :responses {200 {:schema [s/Int]}}}))
