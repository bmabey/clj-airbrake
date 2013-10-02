(ns clj-airbrake.core
  (:use (clj-stacktrace [core :only [parse-exception]] [repl :only [method-str]]))
  (:use [clojure.string :only (split escape)])
  (:use [clojure.data.xml :only [sexp-as-element indent-str]])
  (:require [clj-http.lite.client :as client]
            [org.httpkit.client :as httpclient]
            [clojure.zip :as zip]
            [clojure.xml :as xml]
            [clojure.java.io :as jio]
            [clojure.data.zip.xml :as zf]))

(def api-host
  "Host to send the errors to."
  (atom "airbrakeapp.com"))

(defn set-host! [new-host]
  (reset! api-host new-host))

(defn get-version []
  (or (System/getProperty "clj-airbrake.version")
      (let [props (doto (java.util.Properties.)
                    (.load (jio/reader (jio/resource "META-INF/maven/clj-airbrake/clj-airbrake/pom.properties"))))]
        (.getProperty props "version"))))

(def version (get-version))

(defn xml-ex-response [exception & [message-prefix]]
  (let [{:keys [trace-elems]} (parse-exception exception)
        message (str exception)]
    [:error
     [:class (first (split message #":"))]
     [:message (.trim (str message-prefix " " message))]
     (vec (cons :backtrace
                (for [{:keys [file line], :as elem} trace-elems]
                  [:line {:file file :number line :method (method-str elem)}])))]))

(defn- sanitize
  "converts v to a string and escapes html entities"
  [v]
  (when v
    (escape (name v) {\< "&lt;" \> "&gt;" \& "&amp;" \" "&quot;" \' "&apos;"})))

(defn- map->xml-vars [hash-map sub-map-key]
  (when-let [sub-map (sub-map-key hash-map)]
    (when-not (empty? sub-map)
      (vec (cons sub-map-key
                 (for [[k,v] sub-map]
                   [:var {:key (sanitize k)} (sanitize v)]))))))

(defn make-notice
  ([api-key environment-name project-root exception & [request message-prefix]]
    (indent-str
      (sexp-as-element
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
               [:environment-name (sanitize environment-name)]]]))))

(defn- parse-xml [xml-str]
  (-> xml-str java.io.StringReader. org.xml.sax.InputSource. xml/parse zip/xml-zip))

(defn- get-url [host] (str "http://" (or host @api-host) "/notifier_api/v2/notices"))

(defn handle-response [response]
  (let [body-xml (-> response :body parse-xml)
        text-at (fn [key] (first (zf/xml-> body-xml key zf/text)))]
    {:id (text-at :id)
     :error-id (text-at :error-id)
     :url (text-at :url)}))

(defn send-notice [notice & [host]]
  (-> host
      get-url
      (client/post {:body notice :content-type :xml :accept :xml})
      handle-response))

(defn send-notice-async [callback notice & [host]]
  (httpclient/post (get-url host)
                   {:body notice :content-type :xml :accept :xml}
                   #(-> % handle-response callback)))

(defn ^:dynamic notify [& args]
  (send-notice (apply make-notice args)))

(defn ^:dynamic notify-async [callback & args]
  (send-notice-async callback (apply make-notice args)))
