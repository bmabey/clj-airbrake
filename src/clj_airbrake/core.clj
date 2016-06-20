(ns clj-airbrake.core
  (:use (clj-stacktrace [core :only [parse-exception]] [repl :only [method-str]]))
  (:require [clj-http.lite.client :as httpclient]
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

(defn make-error [message-prefix throwable]
  (let [{:keys [trace-elems]} (parse-exception throwable)]
    {:type (.getName (type throwable))
     :message (str message-prefix (.getMessage throwable))
     :backtrace
     (for [{:keys [file line], :as elem} trace-elems]
       {:line line :file file :function (method-str elem)})}))

(defn make-notice [throwable {:keys [message-prefix context session params environment-name root-directory]}]
  (generate-string
   {:notifier {:name "clj-airbrake"
               :version version
               :url "http://github.com/bmabey/clj-airbrake"}
    :errors [(make-error message-prefix throwable)]
    :context (merge {:os (get-operating-system)
                     :language (str "Clojure-" (clojure-version))
                     :environment environment-name
                     :rootDirectory root-directory}
                    context)
    :environment (System/getenv)
    :session (or session {})
    :params (or params {})}))

(defn- get-url [project api-key]
  (str "https://airbrake.io/api/v3/projects/" project "/notices?key=" api-key))

(defn handle-response [response]
  (-> response :body (parse-string true)))

(defn send-notice [notice project api-key]
  (httpclient/post (get-url project api-key)
                   {:body notice :headers {"Content-Type" "application/json"}}))

(defn is-ignored-environment? [environment ignored-environments]
  (if (coll? ignored-environments)
    (get ignored-environments environment)))

(defn validate-config [{:keys [environment-name api-key project] :as config}]
  ;; Pull in Schema or another validation library?
  (if (or (s/blank? api-key)
          (s/blank? project))
    (throw (IllegalArgumentException. "Airbrake configuration must contain non-empty 'api-key' and 'project'"))))

(defn notify
  ([airbrake-config]
   (partial notify airbrake-config))
  ([airbrake-config throwable]
   (notify airbrake-config throwable {}))
  ([airbrake-config throwable extra-data]
   (let [{:keys [environment-name api-key project ignored-environments root-directory]
          :or {ignored-environments #{"test" "development"}}}
         airbrake-config]
     (validate-config airbrake-config)
     (if (is-ignored-environment? environment-name ignored-environments)
       nil
       (-> (make-notice throwable (merge extra-data {:environment-name environment-name :root-directory root-directory}))
           (send-notice project api-key))))))

(defn notify-async
  {:deprecated "3.1.0"}
  ([airbrake-config throwable]
   (notify-async airbrake-config throwable {}))
  ([airbrake-config throwable extra-data]
   (future (notify airbrake-config throwable extra-data))))

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
