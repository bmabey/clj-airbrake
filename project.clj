(defproject clj-hoptoad "0.1.1"
  :description "Hoptoad Client"
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [clj-http "0.1.1"]
                 [clj-stacktrace "0.2.0"]]
  :dev-dependencies [[enlive "1.0.0-SNAPSHOT"]
                     [swank-clojure "1.2.1"]
                     [midje "0.5.0"]
                     [lein-difftest "1.2.2"]]
  :hooks [leiningen.hooks.difftest])
