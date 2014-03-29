# clj-airbrake

Clojure client for the [Airbrake API](http://www.airbrakeapp.com/pages/home)

## Usage

```clojure
(require '[clj-airbrake.core :as airbrake])

(def request {:url "http://example.com" :component "foo" :action "bar"
             :cgi-data {"SERVER_NAME" "nginx", "HTTP_USER_AGENT" "Mozilla"}
             :params {"city" "LA", "state" "CA"}
             :session {"user-id" "23"}})

(def exception (try (throw (Exception. "Foo")) (catch Exception e e)) ; throw to get a stacktrace

;; send in blocking style using the JDK's http libs
(airbrake/notify "my-api-key" "production" "/app/dir" exception request)
=> {:error-id 42 :id 100 :url "http://sub.airbrakeapp.com/errors/42/notices/100"}


;; send async using http-kit and callbacks
(airbrake/notify-async (fn [resp] ...) "my-api-key" "production" "/app/dir" exception request)

;; the notify and notify-async fns are also dynamic vars if you wish to swap them out for
;; a different implementation using the same XML request helpers in clj-airbrake.core
 ```

Note that the `request` is one-to-one with Airbrake's API and *not* a [Ring][ring] request.  If you are using ring please <a href="#middleware">see below</a> about the provided ring middleware.

The `request` is optional but is the only way the Airbrake API allows you to pass in additonal information.
So, if you are using the client out of the context of a web application you can use the request to send in
enviromental variables or anything else that may help in
troubleshooting.  Note, that `url` in the request is required if you
do provide a `request` hashmap.

### Additional Configuration

If you need to change the host name where the errors are sent you you
can use the `#'set-host!` function:

```clojure
(require '[clj-airbrake.core :as airbrake])

(airbrake/set-host! "api.airbrake.io")

```



## Installation

`clj-airbrake` is available as a Maven artifact [Clojars](http://clojars.org/clj-airbrake).

Leiningen:

```clojure
:dependencies
  [[clj-airbrake "2.4.1"] ...]
```
Maven:

    <dependency>
      <groupId>clj-airbrake</groupId>
      <artifactId>clj-airbrake</artifactId>
      <version>2.4.1</version>
    </dependency>


## Development

Running the tests:

    $ lein deps
    $ lein test

## Ring Middleware
<a name="middleware" />

Basic support for Ring is provided in the `clj-airbrake.ring` namespace: request parameters and session information are passed to Airbrake. A simple ring example:

```clojure
(use 'ring.adapter.jetty)
(use 'ring.middleware.params)
(use 'ring.middleware.stacktrace)
(use 'clj-airbrake.ring)

(defn app [req]
  {:status  200
   :headers {"Content-Type" "text/html"}
   :body    (throw (Exception. "Testing"))})

(run-jetty (-> app
               (wrap-params)
               (wrap-airbrake "MY-API-KEY")
               (wrap-stacktrace))
           {:port 8080})
```

## TODO

 * Param filtering. (e.g. automatically filter out any 'password' params)
 * Allow for certain environments to be ignored... defaulting to `#{"test" "development"}`
 * Configuartion management?  i.e. set api-key once

## License

Copyright (C) 2010 LeadTune and Ben Mabey

Released under the MIT License: <http://www.opensource.org/licenses/mit-license.php>

[ring]: https://github.com/ring-clojure/ring/wiki
