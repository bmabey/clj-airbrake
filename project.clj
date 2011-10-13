(defproject clj-airbrake "0.1.4"
  :description "Airbrake Client"
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [clj-http "0.1.1"]
                 [clj-stacktrace "0.2.0"]
                 [ring/ring-core "0.3.6"]]
  :dev-dependencies [[lein-clojars "0.6.0"]
                     [enlive "1.0.0-SNAPSHOT"]
                     [swank-clojure "1.2.1"]
                     [midje "0.5.0"]])
