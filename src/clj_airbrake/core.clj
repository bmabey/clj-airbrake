 (ns clj-airbrake.core
  (:use (clj-stacktrace [core :only [parse-exception]] [repl :only [method-str]]))
  (:require [org.httpkit.client :as httpclient]
            [clojure.java.io :as jio]
            [clojure.string :as s]
            [cheshire.core :refer :all])
  (:import [java.util.concurrent ThreadPoolExecutor LinkedBlockingQueue TimeUnit]
           [org.httpkit PrefixThreadFactory]))

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

(defn sensitive? [preds [k _]]
  ((apply some-fn preds) k))

(defn scrub [regexes m]
  (let [preds (map #(partial re-matches %)  regexes)]
    (->> m
         (remove (partial sensitive? preds))
         (into {}))))

(defn get-environment-variables [sensitive-environment-variables]
  (scrub sensitive-environment-variables (System/getenv)))

(defn remove-sensitive-params [params sensitive-params]
  (if params
    (scrub sensitive-params params)
    {}))

(defn make-notice [throwable {:keys [message-prefix context session params environment-name root-directory]} sensitive-environment-variables sensitive-params]
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
    :environment (get-environment-variables sensitive-environment-variables)
    :session (or session {})
    :params (remove-sensitive-params params sensitive-params)}))

(defn- get-url [project api-key]
  (str "https://airbrake.io/api/v3/projects/" project "/notices?key=" api-key))

(defn handle-response [response]
  (-> response :body (parse-string true)))

(def thread-pool
  (let [max (.availableProcessors (Runtime/getRuntime))
        queue (LinkedBlockingQueue.)
        factory (PrefixThreadFactory. "airbrake-worker-")]
    (ThreadPoolExecutor. max max 60 TimeUnit/SECONDS queue factory)))

(defn send-notice-async [notice callback project api-key]
  (httpclient/post (get-url project api-key)
                   {:body notice
                    :headers {"Content-Type" "application/json"}
                    :worker-pool thread-pool}
                   #(-> % handle-response callback)))

(defn is-ignored-environment? [environment ignored-environments]
  (if (coll? ignored-environments)
    (get ignored-environments environment)))

(defn validate-config [{:keys [environment-name api-key project] :as config}]
  ;; Pull in Schema or another validation library?
  (if (or (s/blank? api-key)
          (s/blank? project))
    (throw (IllegalArgumentException. "Airbrake configuration must contain non-empty 'api-key' and 'project'"))))

(def defaults
  {:ignored-environments #{"test" "development"}
   :sensitive-environment-variables [#"(?i)PASS" #"(?i)SECRET" #"(?i)TOKEN" #"(?i)AWS_ACCESS_KEY_ID" #"(?i)AWS_SECRET_ACCESS_KEY"]
   :sensitive-params [#"(?i)pass"]})

(defn notify-async
  ([airbrake-config callback throwable]
   (notify-async callback airbrake-config throwable {}))
  ([airbrake-config callback throwable extra-data]
   (let [{:keys [environment-name api-key project ignored-environments root-directory sensitive-environment-variables sensitive-params]} (merge defaults airbrake-config)
         notice-data (merge extra-data {:environment-name environment-name :root-directory root-directory})]
     (validate-config airbrake-config)
     (if (is-ignored-environment? environment-name ignored-environments)
       (future nil)
       (-> (make-notice throwable notice-data sensitive-environment-variables sensitive-params)
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
      ;; should we log here?
      (notify ~airbrake-config
              t#
              ~extra-data)
      (throw t#))))
