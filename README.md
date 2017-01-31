# clj-airbrake

[![Clojars Project](https://img.shields.io/clojars/v/clj-airbrake.svg)](https://clojars.org/clj-airbrake)

Clojure client for the [Airbrake API](http://www.airbrakeapp.com/pages/home)

## Usage

```clojure
(require '[clj-airbrake.core :as airbrake])

(def airbrake-config ..)

(def request {:context {:url "http://example.com"
                        :component "foo"
                        :action "bar"
                        :headers {"SERVER_NAME" "nginx", "HTTP_USER_AGENT" "Mozilla"}}
              :params {"city" "LA", "state" "CA"}
              :session {"user-id" "23"}})

(def exception (try (throw (Exception. "Foo")) (catch Exception e e)) ; throw to get a stacktrace

;; blocking notify
(airbrake/notify airbrake-configuration exception request)
=> {:error-id 42 :id 100 :url "http://sub.airbrakeapp.com/errors/42/notices/100"}

;; async notify
(airbrake/notify-async airbrake-configuration (fn [resp] ...) exception request)

;; wrapper shorthand
(airbrake/with-airbrake airbrake-configuration
                        request
                        ;; your code goes here
                        )
```

Note: `notify` and `notify-async` use http-kit to send the notice. To avoid possible deadlocks with other requests when sending the notice (see issue #30), we create our own threadpool for http-kit to use rather than using the default threadpool.

## Airbrake configuration

Below is an example of the `airbrake-configuration`:

```clojure
{
 :api-key "API_KEY"        ;required
 :project "PROJECT_ID"     ;required
 :host                     ;optional
 :environment-name "env"   ;optional
 :root-dirctory "/app/dir" ;optional

 :ignored-environments #{"test" "development"} ;optional but defaults to 'development' and 'test'
 :sensitive-environment-variables [#"pass"] ;optional
 :sensitive-params [#"pass"] ;optional
 }
```
Both `api-key` and `project` can both be found in the settings for your project in the Airbrake website.

### Senstive data

There are times that your environment and params may contain sensitive data (e.g. passwords) that you don't want sent to airbrake. We try to provide sensible defaults for this but can be configured throught the keys `sensitive-environment-variables` and `sensitive-params`. Both values are a vector of regexes that will be tested against the keys in your environment variable and params.

### Currying

Unsurprisingly, passing the configuration to `notify` for every single call could be painful. So a convinience macro is provided.

```clojure
(def airbrake-config {:api-key "api-key" :project "project"})

(def-notify my-notify airbrake-config)

(my-notify (Exception. "Something went wrong."))
```

Notify is also overloaded so if you just pass the airbrake configuration it will return a function that can be used to send a notification.


## Request

Optionally you can pass a 3rd parameter to `notify` and 4th parameter to `notify-async`

This parameter must be a map and will look for three keys in this map: `session`, `params`, and `context`

`session` and `params` are expected to be maps with any keys.

### Context
Context can contain the following keys:
```clojure
{
 :environment ""   ; will default to `environment-name` from configuration
 :rootDirectory "" ; will default to `root-directory` from configuration
 :os ""            ; will look up operating system
 :language ""      ; will default to "Clojure-1.7.0" (or whichever version of Clojure you're running)
 :component ""
 :action ""
 :version ""
 :url ""
 :userAgent ""
 :userId ""
 :userUsername ""
 :userName ""
 :userEmail ""
}
```

More information about what can be passed to Airbrake can be found in the Airbrake documentation - https://airbrake.io/docs/#create-notice-v3

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
               (wrap-airbrake airbrake-configuration)
               (wrap-stacktrace))
           {:port 8080})
```

## Installation

`clj-airbrake` is available as a Maven artifact [Clojars](http://clojars.org/clj-airbrake).


Leiningen:

```clojure
:dependencies
  [[clj-airbrake "3.0.3"] ...]
```
Maven:

    <dependency>
      <groupId>clj-airbrake</groupId>
      <artifactId>clj-airbrake</artifactId>
      <version>3.0.3</version>
    </dependency>


## Development

Running the tests:

    $ lein deps
    $ lein test


## TODO

 * Param filtering. (e.g. automatically filter out any 'password' params)

## License

Copyright (C) 2010 LeadTune and Ben Mabey

Released under the MIT License: <http://www.opensource.org/licenses/mit-license.php>

[ring]: https://github.com/ring-clojure/ring/wiki
