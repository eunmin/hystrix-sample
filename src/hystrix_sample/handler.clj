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

(def circuit-breaker-open? (atom false))

(defcommand hello-cmd
  {:hystrix/command-key :hello-cmd
   :hystrix/fallback-fn (fn [args] "error")}
  [message]
  (if (not @circuit-breaker-open?)
    (Thread/sleep (rand-int 2000)))
  (if (> (Math/random) 0.8)
    (str "hello " message)
    (throw (IllegalStateException.))))

(defn- get-hystrix-key [cmd-key]
  (HystrixCommandKey$Factory/asKey (name cmd-key)))

(defn- get-metrics [hystrix-cmd-key]
  (HystrixCommandMetrics/getInstance hystrix-cmd-key))

(defn- get-circuit-breaker [hystrix-cmd-key]
  (HystrixCircuitBreaker$Factory/getInstance hystrix-cmd-key))

(defn- set-circuit-breaker-status! []
  (let [hystrix-cmd-key (get-hystrix-key :hello-cmd)]
    (if-let [metrics (get-metrics hystrix-cmd-key)]
      (if-let [cb (get-circuit-breaker hystrix-cmd-key)]
        (swap! circuit-breaker-open? (fn [_] (.isOpen cb)))))))

(defn- set-hystrix-config! [cmd-key k v]
  (let [key (format "hystrix.command.%s.%s" (name cmd-key) k)]
    (.setProperty (ConfigurationManager/getConfigInstance) key v)))

(defn- init-hystrix! []
  (set-hystrix-config! :hello-cmd "circuitBreaker.requestVolumeThreshold" 5)
  (set-hystrix-config! :hello-cmd "circuitBreaker.errorThresholdPercentage" 50)
  (set-hystrix-config! :hello-cmd "execution.isolation.thread.timeoutInMilliseconds" 1000))

(defn init []
  (init-hystrix!)
  (HystrixRequestContext/initializeContext))

(defn destroy []
  (when-let [c (HystrixRequestContext/getContextForCurrentThread)]
    (.shutdown c)))

(defroutes app-routes
  (GET "/" []
       (set-circuit-breaker-status!)
       (println "circuit breaker open: " @circuit-breaker-open?)
       (hello-cmd "world"))
  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes site-defaults))
