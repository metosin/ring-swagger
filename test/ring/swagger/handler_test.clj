(ns ring.swagger.handler-test
  (:require [midje.sweet :refer :all]
            [ring.swagger.handler :refer :all]
            [schema.core :as s]))

(fact "handler with meta"
  (let [metadata {:tags         [:programmer]
                  :summary      "programming endpoint"
                  :description  "no gardening here"
                  :externalDocs {:url "http://www.metosin.fi"
                                 :description "link to it"}
                  :parameters   {:query    {:language s/Str, :level s/Int}
                                 :path     {:id s/Str}}
                  :responses    {200      {:description "ok"}
                                 :default {:description "error"
                                           :schema      {:code Long}}}}
        endpoint (handler metadata identity)]
    (endpoint ..request..) => ..request..
    (meta endpoint) => metadata))


