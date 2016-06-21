(defproject clj-airbrake "3.1.0"
  :description "Airbrake Client"
  :min-lein-version "2.0.0"
  :url "https://github.com/bmabey/clj-airbrake"
  :dependencies [[clj-http-lite "0.3.0"]
                 [clj-stacktrace "0.2.8"]
                 [ring/ring-core "1.2.0"]
                 [org.clojure/clojure "1.7.0"]
                 [cheshire "5.5.0"]]
  :profiles {:dev
             {:resource-paths ["test-resources"]
              :dependencies [[enlive "1.1.4"]]}})
