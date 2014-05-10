(ns ring.swagger.test-utils)

(defn fake-servlet-context [context]
 (proxy [javax.servlet.ServletContext] []
   (getContextPath [] context)))
