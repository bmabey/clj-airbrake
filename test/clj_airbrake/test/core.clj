(ns clj-airbrake.test.core
  (:use [clj-airbrake.core] :reload)
  (:use clojure.test))

(deftest test-make-notice
  (with-redefs [cheshire.core/generate-string identity]
    (let [notifier {:name "clj-airbrake"
                    :version version
                    :url "http://github.com/leadtune/clj-airbrake"}]
      (= notifier (:notifer (make-notice "" (Exception.) {}))))))

(deftest test-make-error
  (let [e (Exception. "Something went wrong")]
    (= "Exception" (:type (make-error e)))
    (= "Something went wrong" (:message (make-error e)))
    (= [] (:backtrace (make-error e)))))

(deftest test-ignored-environments
  (let [notice-args (atom nil)]
    (with-redefs [clj-airbrake.core/send-notice-async (fn [& args] (future (reset! notice-args args)))]
      (notify {:api-key "api-key" :environment-name "development" :project "p"} (Exception.))
      (is (= nil @notice-args))

      (notify {:api-key "api-key" :environment-name "production" :project "p"} (Exception.))
      (is (not (= nil @notice-args))))))


