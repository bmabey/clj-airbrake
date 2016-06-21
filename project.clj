(defproject clj-airbrake "3.0.4"
  :description "Airbrake Client"
  :min-lein-version "2.0.0"
  :url "https://github.com/bmabey/clj-airbrake"
  :dependencies [[http-kit "2.1.19"]
                 [clj-stacktrace "0.2.8"]
                 [ring/ring-core "1.2.0"]
                 [org.clojure/clojure "1.7.0"]
                 [cheshire "5.5.0"]]
  :profiles {:dev
             {:resource-paths ["test-resources"]
              :dependencies [[enlive "1.1.4"]]}})
