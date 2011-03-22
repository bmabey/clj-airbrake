(ns clj-hoptoad.ring
  (:use clj-hoptoad.core)
  (:require [clojure.contrib.java-utils :as j]))

(defn request-to-message
  "Maps the ring request map to the format of the hoptoad params"
  [req]
  {:url (str (name (:scheme req))
             "://"
             (:server-name req)
             (:uri req))
   :component "component"
   :action "action"
   :cgi-data {}
   :params (or (:params req)
               {:query-string (:query-string req)})
   :session (or (:session req)
                {})})

(defn wrap-hoptoad
  "Catches exceptions and sends Hoptoad notification."
  ([handler api-key]
     (wrap-hoptoad handler api-key "development"))
  ([handler api-key environment-name]
     (wrap-hoptoad handler api-key environment-name request-to-message))
  ([handler api-key environment-name request-mapper]
     (fn [req]
       (try (handler req)
            (catch Exception e
              (notify api-key
                      environment-name
                      (j/get-system-property "user.dir")
                      e
                      (request-mapper req))
              (throw e))))))
