(ns clj-hoptoad.core
  (use (clj-stacktrace [core :only [parse-exception]] [repl :only [method-str]])
       (clojure.contrib [string :only [split]] prxml))
  (require [clj-http.client :as client]
           [clojure.zip :as zip]
           [clojure.xml :as xml]
           [clojure.contrib.zip-filter.xml :as zf]))

(def version "0.1.0") ; Can I get this from project.clj?

(defn- xml-ex-response [exception]
  (let [{:keys [trace-elems]} (parse-exception exception)
        message (str exception)]
    [:error
     [:class (first (split #":" message))]
     [:message message]
     (vec (cons :backtrace
                (for [{:keys [file line], :as elem} trace-elems]
                  [:line {:file file :number line :method (method-str elem)}])))]))

(defn- keyword-check
  "prxml barfs with symbols so we need to convert values to strings"
  [v]
  (if (keyword? v)
    (name v)
    v))

(defn- map->xml-vars [hash-map sub-map-key]
  (when-let [sub-map (sub-map-key hash-map)]
    (when-not (empty? sub-map)
      (vec (cons sub-map-key
                 (for [[k,v] sub-map]
                   [:var {:key k} (keyword-check v)]))))))

(defn make-notice
  ([api-key environment-name project-root exception]
    (make-notice api-key environment-name project-root exception nil))
  ([api-key environment-name project-root exception request]
    (binding [*prxml-indent* 2]
      (with-out-str
        (prxml [:decl! "1.0"]
               [:notice {:version "2.0"}
                [:api-key api-key]
                [:notifier
                 [:name "clj-hoptoad"]
                 [:version version]
                 [:url "http://github.com/leadtune/clj-hoptoad"]]
                (xml-ex-response exception)
                (when request
                  (when-not (:url request)
                    (throw (IllegalArgumentException. ":url is required when passing in a request")))
                  [:request
                   [:url (keyword-check (:url request))]
                   [:component (keyword-check (:component request))]
                   [:action (keyword-check (:action request))]
                   (map->xml-vars request :cgi-data)
                   (map->xml-vars request :params)
                   (map->xml-vars request :session)])
                [:server-environment
                 [:project-root (keyword-check project-root)]
                 [:environment-name (keyword-check environment-name)]]])))))

(defn- parse-xml [xml-str]
  (-> xml-str java.io.StringReader. org.xml.sax.InputSource. xml/parse zip/xml-zip))

(defn send-notice [notice]
  (let [response (client/post "http://hoptoadapp.com/notifier_api/v2/notices"
                              {:body notice :content-type :xml :accept :xml})
        body-xml (-> response :body parse-xml)
        text-at (fn [key] (first (zf/xml-> body-xml key zf/text)))]
    {:id (Integer. (text-at :id))
     :error-id (Integer. (text-at :error-id))
     :url (text-at :url)}))

(defn notify [& args]
  (send-notice (apply make-notice args)))
