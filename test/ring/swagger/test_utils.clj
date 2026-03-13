(ns ring.swagger.test-utils)

(defn fake-servlet-context [context]
 (proxy [javax.servlet.ServletContext] []
   (getContextPath [] context)))

(defn fake-jakarta-servlet-context [context]
  (proxy [jakarta.servlet.ServletContext] []
    (getContextPath [] context)))
