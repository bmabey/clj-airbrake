(defproject clj-airbrake "2.3.0-SNAPSHOT"
  :description "Airbrake Client"
  :min-lein-version "2.0.0"
  :url "https://github.com/bmabey/clj-airbrake"
  :dependencies [[clj-http-lite "0.2.0"]
                 [http-kit "2.1.10"]
                 [clj-stacktrace "0.2.6"]
                 [ring/ring-core "0.3.6"]
                 [org.clojure/data.zip "0.1.1"]
                 [org.clojure/clojure "1.4.0"]
                 [org.clojure/data.xml "0.0.7"]]
  :profiles {:dev
             {:resource-paths ["test-resources"],
              :dependencies
              [[enlive "1.0.0-SNAPSHOT"]
               [midje "1.3.1"]]}})
