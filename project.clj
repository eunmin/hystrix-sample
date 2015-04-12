(defproject hystrix-sample "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [compojure "1.3.1"]
                 [ring/ring-defaults "0.1.2"]
                 [com.netflix.hystrix/hystrix-clj "1.4.4"]]
  :plugins [[lein-ring "0.8.13"]]
  :ring {:init hystrix-sample.handler/init
         :handler hystrix-sample.handler/app
         :destroy hystrix-sample.handler/destroy}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]]}})
