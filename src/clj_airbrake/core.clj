(ns clj-airbrake.core
  (:use (clj-stacktrace [core :only [parse-exception]] [repl :only [method-str]]))
  (:require [org.httpkit.client :as httpclient]
            [clojure.java.io :as jio]
            [clojure.string :as s]
            [cheshire.core :refer :all]))

(defn get-version []
  (or (System/getProperty "clj-airbrake.version")
      (let [props (doto (java.util.Properties.)
                    (.load (jio/reader (jio/resource "META-INF/maven/clj-airbrake/clj-airbrake/pom.properties"))))]
        (.getProperty props "version"))))

(defn get-operating-system []
  (str (System/getProperty "os.name") " " (System/getProperty "os.version") " " (System/getProperty "os.arch")))

(def version (get-version))

(defn make-error [throwable]
  (let [{:keys [trace-elems]} (parse-exception throwable)
        message (str throwable)]
    {:type (str throwable)
     :message message
     :backtrace
     (for [{:keys [file line], :as elem} trace-elems]
       {:line line :file file :method (method-str elem)})}))

(defn make-notice [environment-name throwable {:keys [message-prefix context session params]}]
  (generate-string
   {:notifier {:name "clj-airbrake"
               :version version
               :url "http://github.com/leadtune/clj-airbrake"}
    :errors [(make-error throwable)]
    :context (merge {:os (get-operating-system)
                     :language (str "Clojure-" (clojure-version))
                     :environment environment-name}
                    context)
    :environment (System/getenv)
    :session (or session {})
    :params (or params {})}))

(defn- get-url [project api-key]
  (str "https://airbrake.io/api/v3/projects/" project "/notices?key=" api-key))

(defn handle-response [response]
  (-> response :body (parse-string true)))

(defn send-notice-async [notice callback project api-key]
  (httpclient/post (get-url project api-key)
                   {:body notice :content-type :json :accept :json}
                   #(-> % handle-response callback)))

(defn is-ignored-environment? [environment ignored-environments]
  (if (coll? ignored-environments)
    (get ignored-environments environment)))

(defn validate-config [{:keys [environment-name api-key project] :as config}]
  ;; Pull in Schema or another validation library?
  (if (or (s/blank? api-key)
          (s/blank? project))
    (throw (IllegalArgumentException. "Airbrake configuration must contain non-empty 'api-key' and 'project'"))))

(defn notify-async
  ([airbrake-config callback throwable]
   (notify-async callback airbrake-config throwable {}))
  ([airbrake-config callback throwable extra-data]
   (let [{:keys [environment-name api-key project ignored-environments]
          :or {ignored-environments #{"test" "development"}}}
         airbrake-config]
     (validate-config airbrake-config)
     (if (is-ignored-environment? environment-name ignored-environments)
       (future nil)
       (-> (make-notice environment-name throwable extra-data)
           (send-notice-async callback project api-key))))))

(defn notify
  ([airbrake-config]
   (partial notify airbrake-config))
  ([airbrake-config throwable]
   (notify airbrake-config throwable {}))
  ([airbrake-config throwable extra-data]
   @(notify-async airbrake-config identity throwable extra-data)))

(defmacro def-notify [name airbrake-config]
  `(def ~name (notify ~airbrake-config)))

(defmacro with-airbrake [airbrake-config extra-data & body]
  `(try
    ~@body
    (catch Throwable t#
      (notify ~airbrake-config
              t#
              ~extra-data)
      (throw t#))))
