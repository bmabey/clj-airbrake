(ns clj-airbrake.core
  (use (clj-stacktrace [core :only [parse-exception]] [repl :only [method-str]])
       (clojure.contrib [string :only [split escape as-str]] prxml))
  (require [clj-http.client :as client]
           [clojure.zip :as zip]
           [clojure.xml :as xml]
           [clojure.contrib.zip-filter.xml :as zf]))

(def version "0.1.2") ; Can I get this from project.clj? Answer: currently only by loading and parsing project.clj

(defn- xml-ex-response [exception & [message-prefix]]
  (let [{:keys [trace-elems]} (parse-exception exception)
        message (str exception)]
    [:error
     [:class (first (split #":" message))]
     [:message (.trim (str message-prefix " " message))]
     (vec (cons :backtrace
                (for [{:keys [file line], :as elem} trace-elems]
                  [:line {:file file :number line :method (method-str elem)}])))]))

(defn- sanitize
  "converts v to a string and escapes html entities"
  [v]
  (escape {\< "&lt;" \> "&gt;" \& "&amp;" \" "&quot;" \' "&apos;"} (as-str v)))

(defn- map->xml-vars [hash-map sub-map-key]
  (when-let [sub-map (sub-map-key hash-map)]
    (when-not (empty? sub-map)
      (vec (cons sub-map-key
                 (for [[k,v] sub-map]
                   [:var {:key (sanitize k)} (sanitize v)]))))))

(defn make-notice
  ([api-key environment-name project-root exception & [request message-prefix]]
    (binding [*prxml-indent* 2]
      (with-out-str
        (prxml [:decl! "1.0"]
               [:notice {:version "2.0"}
                [:api-key api-key]
                [:notifier
                 [:name "clj-airbrake"]
                 [:version version]
                 [:url "http://github.com/leadtune/clj-airbrake"]]
                (xml-ex-response exception message-prefix)
                (when request
                  (when-not (:url request)
                    (throw (IllegalArgumentException. ":url is required when passing in a request")))
                  [:request
                   [:url (sanitize (:url request))]
                   [:component (sanitize (:component request))]
                   [:action (sanitize (:action request))]
                   (map->xml-vars request :cgi-data)
                   (map->xml-vars request :params)
                   (map->xml-vars request :session)])
                [:server-environment
                 [:project-root (sanitize project-root)]
                 [:environment-name (sanitize environment-name)]]])))))

(defn- parse-xml [xml-str]
  (-> xml-str java.io.StringReader. org.xml.sax.InputSource. xml/parse zip/xml-zip))

(defn send-notice [notice]
  (let [response (client/post "http://airbrakeapp.com/notifier_api/v2/notices"
                              {:body notice :content-type :xml :accept :xml})
        body-xml (-> response :body parse-xml)
        text-at (fn [key] (first (zf/xml-> body-xml key zf/text)))]
    {:id (BigInteger. (text-at :id))
     :error-id (BigInteger. (text-at :error-id))
     :url (text-at :url)}))

(defn notify [& args]
  (send-notice (apply make-notice args)))
