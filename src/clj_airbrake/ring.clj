(ns clj-airbrake.ring
  (:use clj-airbrake.core)
  (:require [ring.util.codec :as codec :refer [url-decode]]))

(defn request-to-message
  "Maps the ring request map to the format of the airbrake params"
  [{:keys [query-string] :as req}]
  {:url (str (name (:scheme req))
             "://"
             (:server-name req)
             (:uri req)
             (when query-string (str "?" (codec/url-decode query-string))))
   :component "component"
   :action "action"
   :cgi-data (get req :headers {})
   :params (or (:params req)
               {:query-string query-string})
   :session (get req :session {})})

(defn wrap-airbrake
  "Catches exceptions and sends Airbrake notification."
  ([handler api-key]
     (wrap-airbrake handler api-key "development"))
  ([handler api-key environment-name]
     (wrap-airbrake handler api-key environment-name request-to-message))
  ([handler api-key environment-name request-mapper]
     (fn [req]
       (with-airbrake {:api-key api-key
                       :environment-name environment-name
                       :project-root (System/getProperty "user.dir")}
                      (request-mapper req)
                      (handler req)
                      ))))
