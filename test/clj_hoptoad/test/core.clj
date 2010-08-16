(ns clj-hoptoad.test.core
  (:use [clj-hoptoad.core] :reload-all)
  (:use clojure.contrib.zip-filter.xml
        clojure.test)
  (:require
            [clojure.zip :as zip]
            [clojure.xml :as xml]))

(defn parse-xml [xml-str]
  (-> xml-str java.io.StringReader. org.xml.sax.InputSource. xml/parse zip/xml-zip))

(defn make-notice-zip [& args]
  ;(println (apply make-notice args))
  (parse-xml (apply make-notice args)))

(defn backtrace-lines [notice-xml]
   (map :attrs (xml-> notice-xml :error :backtrace :line zip/node)))

(deftest test-make-notice
  (testing "error section"
    (let [exception (try (throw (Exception. "Foo")) (catch Exception e e))
          notice-xml (-> (make-notice-zip exception))]
      (is (= '("java.lang.Exception") (xml-> notice-xml :error :class text)))
      (is (= '("java.lang.Exception: Foo") (xml-> notice-xml :error :message text)))
      (let [first-line (first (backtrace-lines notice-xml))]
        (is (= "core.clj" (:file first-line)))
        (is (= "clj-hoptoad.test.core/fn[fn]" (:method first-line)))
        (is (re-matches #"^\d+$" (:number first-line)))))))
