# clj-hoptoad

Clojure client for the [Hoptoad API](http://hoptoadapp.com/pages/home).

## Usage

    (require '[clj-hoptoad.core :as hoptoad])

    (def request {:url "http://example.com" :component "foo" :action "bar"
                 :cgi-data {"SERVER_NAME" "nginx", "HTTP_USER_AGENT" "Mozilla"}
                 :params {"city" "LA", "state" "CA"}
                 :session {"user-id" "23"}})

    (def exception (try (throw (Exception. "Foo")) (catch Exception e e)) ; throw to get a stacktrace

    (hoptoad/notify "my-api-key" "production" "/app/dir" exception request)
    => {:error-id 42 :id 100 :url "http://sub.hoptoadapp.com/errors/42/notices/100"}

Note that the `request` is one-to-one with Hoptoad's API and *not* a [Ring][ring] request.  If you are using ring please <a href="#middleware">see below</a> about the provided ring middleware.

The `request` is optional but is the only way the Hoptoad API allows you to pass in additonal information.
So, if you are using the client out of the context of a web application you can use the request to send in
enviromental variables or anything else that may help in troubleshooting.  Note, that `url` in the request is required if you do provide a `request` hashmap.


## Installation

`clj-hoptoad` is available as a Maven artifact [Clojars](http://clojars.org/clj-hoptoad).

Leiningen:

    :dependencies
      [[clj-hoptoad "0.1.3"] ...]

Maven:

    <dependency>
      <groupId>clj-hoptoad</groupId>
      <artifactId>clj-hoptoad</artifactId>
      <version>0.1.3</version>
    </dependency>


## Development

Running the tests:

    $ lein deps
    $ lein test

## Ring Middleware
<a name="middleware" />

Basic support for Ring is provided in the `clj-hoptoad.ring` namespace: request parameters and session information are passed to Hoptoad. A simple ring example:

    (use 'ring.adapter.jetty)
    (use 'ring.middleware.params)
    (use 'ring.middleware.stacktrace)
    (use 'clj-hoptoad.ring)

    (defn app [req]
      {:status  200
       :headers {"Content-Type" "text/html"}
       :body    (throw (Exception. "Testing"))})

    (run-jetty (-> app
                   (wrap-params)
                   (wrap-hoptoad "MY-API-KEY")
                   (wrap-stacktrace))
               {:port 8080})

## TODO

 * Param filtering. (i.e. automatically filter out any 'password' params)
 * Allow for certain environments to be ignored... defaulting to `#{"test" "development"}`
 * Configuartion management?  i.e. set api-key once

## License

Copyright (C) 2010 LeadTune and Ben Mabey

Released under the MIT License: <http://www.opensource.org/licenses/mit-license.php>

[ring]: http://example.com/
