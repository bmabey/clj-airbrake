(ns clj-hoptoad.core
  (use (clj-stacktrace [core :only [parse-exception]] [repl :only [method-str]])
       (clojure.contrib [string :only [split]] prxml))
  (require [clj-http.client :as client]))

(use 'clojure.contrib.prxml)

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

(defn- map->xml-vars [hash-map sub-map-key]
  (vec (cons sub-map-key
             (for [[k,v] (sub-map-key hash-map)]
               [:var {:key k} v]))))

(defn send-notice [notice]
  (client/post "http://hoptoadapp.com/notifier_api/v2/notices" {:body notice :content-type :xml :accept :xml}))

(defn make-notice [api-key environment-name project-root exception request]
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
              [:request
               [:url (:url request)] ; required if there is a request
               [:component (:component request)]
               [:action (:action request)]
               (map->xml-vars request :cgi-data)
               (map->xml-vars request :params)
               (map->xml-vars request :session)]
              [:server-environment
               [:project-root project-root]
               [:environment-name environment-name]]]))))
