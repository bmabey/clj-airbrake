(ns clj-airbrake.ring
  (:use clj-airbrake.core)
  (:use [ring.util.codec :only (url-decode)]))

(defn request-to-message
  "Maps the ring request map to the format of the airbrake params"
  [{:keys [query-string] :as req}]
  {:url (str (name (:scheme req))
             "://"
             (:server-name req)
             (:uri req)
             (if query-string (str "?" (url-decode query-string))))
   :component "component"
   :action "action"
   :cgi-data (get req :headers {})
   :params (or (:params req)
               {:query-string query-string})
   :session (get req :sesion {})})

(defn wrap-airbrake
  "Catches exceptions and sends Airbrake notification."
  ([handler api-key]
     (wrap-airbrake handler api-key "development"))
  ([handler api-key environment-name]
     (wrap-airbrake handler api-key environment-name request-to-message))
  ([handler api-key environment-name request-mapper]
     (fn [req]
       (try (handler req)
            (catch Throwable t
              (notify api-key
                      environment-name
                      (System/getProperty "user.dir")
                      t
                      (request-mapper req))
              (throw t))))))
