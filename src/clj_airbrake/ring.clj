(ns clj-airbrake.ring
  (:use clj-airbrake.core)
  (:require [ring.util.codec :as codec :refer [url-decode]]))

(defn request-to-message
  "Maps the ring request map to the format of the airbrake params"
  [{:keys [query-string] :as req}]
  {:context {:url (str (name (:scheme req))
                       "://"
                       (:server-name req)
                       (:uri req)
                       (when query-string (str "?" (codec/url-decode query-string))))
             :headers (get req :headers {})}
   :params (or (:params req)
               {:query-string query-string})
   :session (get req :session {})})

(defn wrap-airbrake
  "Catches exceptions and sends Airbrake notification."
  ([handler airbrake-config]
     (wrap-airbrake handler airbrake-config request-to-message))
  ([handler airbrake-config request-mapper]
     (fn [req]
       (with-airbrake airbrake-config (request-mapper req)
         (handler req)))))
