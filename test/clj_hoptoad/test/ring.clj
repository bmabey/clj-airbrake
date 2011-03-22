(ns clj-hoptoad.test.ring
  (:use [clj-hoptoad.ring] :reload)
  (:use clojure.test))

(def request {:remote-addr "127.0.0.1"
              :scheme :http
              :request-method :get
              :query-string nil
              :content-type "application/json"
              :uri "/"
              :server-name "localhost"
              :headers {:accept-language "en-US"
                        :accept-encoding "gzip, deflate"
                        :content-length "1028"
                        :content-type "application/json"
                        :host "localhost:3000"
                        :user-agent "firefox"}
              :content-length 1028
              :server-port 8080
              :character-encoding nil})

(defn- call-app
  [app req]
  (let [environment-name "production"]
    ((wrap-hoptoad app "api-key" environment-name) req)))

(deftest test-wrap-hoptoad
  (testing "returns response when no exception thrown"
    (let [app (fn [req] "response")]
      (is (= "response"
             (call-app app request)))))

  (testing "re-throws exception"
    (let [app (fn [req] (/ 1 0))]
      (binding [clj-hoptoad.core/notify (fn [& args] nil)]
        (is (thrown? ArithmeticException
                     (call-app app request))))))

  (testing "notifies hoptoad with request params"
    (let [app (fn [req] (/ 1 0))
          notify-params (atom nil)
          hoptoad-message (request-to-message request)]
      (binding [clj-hoptoad.core/notify (fn [& args] (reset! notify-params args))]
        (try (call-app app request)
             (catch ArithmeticException e))
        (is (= hoptoad-message
               (last @notify-params)))))))


(deftest test-request-to-message
  (let [message (request-to-message request)]
    (is (= "http://localhost/"
           (:url message)))
    (is (= "component"
           (:component message)))
    (is (= "action"
           (:action message)))
    (testing "params"
      (is (= {:query-string nil}
             (:params message)))
      (is (= {:query-string "blah=yes"}
             (:params (request-to-message (assoc request :query-string "blah=yes")))))
      (is (= {:foo "bar"}
             (:params (request-to-message (assoc request
                                            :params {:foo "bar"}))))))
    (is (= {}
           (:session message)))
    (is (= {:accept-language "en-US"
            :accept-encoding "gzip, deflate"
            :content-length "1028"
            :content-type "application/json"
            :host "localhost:3000"
            :user-agent "firefox"}
           (:cgi-data message)))))
