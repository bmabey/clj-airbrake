(ns clj-hoptoad.core
  (use (clj-stacktrace [core :only [parse-exception]] [repl :only [method-str]])
       (clojure.contrib [string :only [split]] prxml)))

(use 'clojure.contrib.prxml)

(def version "0.1.0")

(defn- xml-ex-response [exception]
  (let [{:keys [trace-elems]} (parse-exception exception)
        message (str exception)]
    [:error
     [:class (first (split #":" message))]
     [:message message]
     (vec (cons :backtrace
                (for [{:keys [file line], :as elem} trace-elems]
                  [:line {:file file :number line :method (method-str elem)}])))]))

(defn make-notice [exception]
  (binding [*prxml-indent* 2]
    (with-out-str
      (prxml [:decl! "1.0"]
             [:notice {:version "2.0"}
              [:api-key "foo"]
              [:notifier
               [:name "clj-hoptoad"]
               [:version version]
               [:url "http://github.com/leadtune/clj-hoptoad"]]
              (xml-ex-response exception)
              [:request
               [:url "http://example.com"]
               [:component]
               [:action]
               [:cgi-data
                [:var {:key "SERVER_NAME"} "example.org"]
                [:var {:key "HTTP_USER_AGENT"} "Mozilla"]]
               [:params
                [:var {:key "login"} "jimmy"]
                [:var {:key "password"} "[FILTERED]"]]
               [:session
                [:var {:key "user_id"} "23"]
                [:var {:key "foo"} "bar"]]]
              [:server-environment
               [:project-root "/testapp"]
               [:environment-name "production"]]]))))
