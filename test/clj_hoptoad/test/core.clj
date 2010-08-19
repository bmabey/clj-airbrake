(ns clj-hoptoad.test.core
  (:use [clj-hoptoad.core] :reload)
  (:use clojure.contrib.zip-filter.xml
        clojure.test
        midje.semi-sweet)
  (:require
            [clojure.zip :as zip]
            [clojure.xml :as xml]
            [clj-http.client :as client]))

(defn- parse-xml [xml-str]
  (-> xml-str java.io.StringReader. org.xml.sax.InputSource. xml/parse zip/xml-zip))

(defn- make-notice-zip [& args]
  ;(println (apply make-notice args))
  (parse-xml (apply make-notice args)))

(defn- backtrace-lines [notice-xml]
  (map :attrs (xml-> notice-xml :error :backtrace :line zip/node)))

(defn- text-in
  [notice-xml path]
  (first (apply xml-> notice-xml (conj path text))))

(defn- var-elems-at
  "Extracts key-values (into a map) from XML blocks like:
    <cgi-data>
      <var key='SERVER_NAME'>example.org</var>
      <var key='HTTP_USER_AGENT'>Mozilla</var>
    </cgi-data>
"
  [notice-xml path]
  (let [pairs (for [var-elem (apply xml-> notice-xml (conj path :var zip/node))]
                [(get-in var-elem [:attrs :key]) (first (:content var-elem))])]
    (apply hash-map (flatten pairs))))


(deftest test-make-notice
  (let [exception (try (throw (Exception. "Foo")) (catch Exception e e))
        request {:url "http://example.com", :component :foo, :action :bar, ; note the symbols... prxml has issues
                 :cgi-data {"SERVER_NAME" "nginx", "HTTP_USER_AGENT" "Mozilla"}
                 :params {"city" "LA", "state" "CA"}
                 :session {"user-id" "23"}}
        notice-xml (make-notice-zip "my-api-key" :production "/testapp" exception request)]
    (are [expected-text path] (= expected-text (text-in notice-xml path))
         "my-api-key" [:api-key]
         "java.lang.Exception" [:error :class]
         "java.lang.Exception: Foo" [:error :message]
         "/testapp" [:server-environment :project-root]
         "production" [:server-environment :environment-name]
         "http://example.com" [:request :url]
         "foo" [:request :component]
         "bar" [:request :action])
    (are [expected-vars path] (= expected-vars (var-elems-at notice-xml path))
         (:cgi-data request) [:request :cgi-data]
         (:params request) [:request :params]
         (:session request) [:request :session])
    (testing "backtraces"
      (let [first-line (first (backtrace-lines notice-xml))]
        (is (= "core.clj" (:file first-line)))
        (is (= "clj-hoptoad.test.core/fn[fn]" (:method first-line)))
        (is (re-matches #"^\d+$" (:number first-line))))))
  (testing "when no request is provided"
    (let [notice-xml (make-notice-zip "my-api-key" "test" "/testapp" (Exception. "foo"))]
      (is (empty? (xml-> notice-xml :request)))))
  (testing "when a request is provided but no URL"
    (let [notice-xml-args ["my-api-key" "test" "/testapp" (Exception. "foo") {:action "foo"}]]
      (is (thrown-with-msg? IllegalArgumentException #"url is required" (apply make-notice notice-xml-args)))))
  (testing "when no session, cgi, or params are provided"
    (let [notice-xml (make-notice-zip "my-api-key" "test" "/testapp" (Exception. "foo") {:url "foo" :session nil :params {}})]
      (is (seq (xml-> notice-xml :request)))
      (is (empty? (xml-> notice-xml :request :session)))
      (is (empty? (xml-> notice-xml :request :params)))
      (is (empty? (xml-> notice-xml :request :cgi-data))))))


(deftest test-send-notice
  (expect (send-notice "<notice>...</notice>") => {:error-id 42 :id 100 :url "http://sub.hoptoadapp.com/errors/42/notices/100"}
          (fake (client/post
                 "http://hoptoadapp.com/notifier_api/v2/notices" {:body "<notice>...</notice>", :content-type :xml, :accept :xml}) =>
                 
                 {:status 200, :headers {"server" "nginx/0.6.35"},
                  :body "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<notice>\n  <error-id type=\"integer\">42</error-id>\n  <url>http://sub.hoptoadapp.com/errors/42/notices/100</url>\n  <id type=\"integer\">100</id>\n</notice>\n"})))
