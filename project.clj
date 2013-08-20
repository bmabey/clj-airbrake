(defproject clj-airbrake "2.1.0"
  :description "Airbrake Client"
  :dependencies [[clj-http "0.7.6"]
                 [clj-stacktrace "0.2.6"]
                 [ring/ring-core "0.3.6"]
                 [org.clojure/data.zip "0.1.1"]
                 [org.clojure/clojure "1.4.0"]
                 [org.clojure/data.xml "0.0.7"]]
  :dev-dependencies [[lein-clojars "0.6.0"]
                     [enlive "1.0.0-SNAPSHOT"]
                     [swank-clojure "1.3.4"]
                     [midje "1.3.1"]])
