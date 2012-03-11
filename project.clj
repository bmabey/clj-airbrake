(defproject clj-airbrake "1.0.0"
  :description "Airbrake Client"
  :dependencies [[clj-http "0.2.7"]
                 [clj-stacktrace "0.2.4"]
                 [ring/ring-core "0.3.6"]
                 [org.clojure/data.zip "0.1.0"]
                 [org.clojure/clojure "1.3.0"]
                 [prxml "1.3.1"]]
  :dev-dependencies [[lein-clojars "0.6.0"]
                     [enlive "1.0.0-SNAPSHOT"]
                     [swank-clojure "1.3.4"]
                     [midje "1.3.1"]])
