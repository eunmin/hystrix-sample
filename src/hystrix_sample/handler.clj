(ns hystrix-sample.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [com.netflix.hystrix.core :refer :all]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]])
  (:import [com.netflix.hystrix.strategy.concurrency HystrixRequestContext]
           [com.netflix.config ConfigurationManager]
           [com.netflix.hystrix HystrixCommandKey]
           [com.netflix.hystrix HystrixCommandKey$Factory]
           [com.netflix.hystrix HystrixCommandMetrics]
           [com.netflix.hystrix HystrixCircuitBreaker$Factory]))

(defcommand hello-cmd
  {:hystrix/command-key :hello-cmd
   :hystrix/fallback-fn (fn [args] "error")}
  [message]
  (Thread/sleep 2000)
  (if (> (Math/random) 0.5)
    (str "hello " message)
    (throw (IllegalStateException.))))

(defn show-status []
  (let [metrics (HystrixCommandMetrics/getInstance (HystrixCommandKey$Factory/asKey "hello-cmd"))
        circuit-breaker (if metrics (HystrixCircuitBreaker$Factory/getInstance (.getCommandKey metrics)))]
    (if circuit-breaker
      (println (.isOpen circuit-breaker))
      (println "null"))))

(defroutes app-routes
  (GET "/" []
       (show-status)
       (hello-cmd "world"))
  (route/not-found "Not Found"))

(defn set-config []
  (.setProperty (ConfigurationManager/getConfigInstance) "hystrix.command.hello-cmd.circuitBreaker.requestVolumeThreshold" 10)
;  (.setProperty (ConfigurationManager/getConfigInstance) "hystrix.command.hello-cmd.circuitBreaker.enabled" false)
  (.setProperty (ConfigurationManager/getConfigInstance) "hystrix.command.hello-cmd.circuitBreaker.errorThresholdPercentage" 50)
  (.setProperty (ConfigurationManager/getConfigInstance) "hystrix.command.hello-cmd.execution.isolation.thread.timeoutInMilliseconds" 1000))

(defn init []
  (println "init")
  (set-config)
  (HystrixRequestContext/initializeContext))

(defn destroy []
  (println "destory")
  (when-let [c (HystrixRequestContext/getContextForCurrentThread)]
    (.shutdown c)))

(def app
  (wrap-defaults app-routes site-defaults))
