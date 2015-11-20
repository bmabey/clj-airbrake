(defproject clj-airbrake "2.4.3"
  :description "Airbrake Client"
  :min-lein-version "2.0.0"
  :url "https://github.com/bmabey/clj-airbrake"
  :dependencies [[clj-http-lite "0.3.0"]
                 [http-kit "2.1.11"]
                 [clj-stacktrace "0.2.6"]
                 [ring/ring-core "1.2.0"]
                 [org.clojure/clojure "1.7.0"]
                 [cheshire "5.5.0"]]
  :profiles {:dev
             {:resource-paths ["test-resources"],
              :dependencies
              [[enlive "1.1.4"]
               [midje "1.5.1"]]}})
