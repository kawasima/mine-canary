(ns mine-canary.publisher
  (:import [backtype.storm StormSubmitter LocalCluster])
  (:require [langohr.queue :as lq]
            [langohr.core :as rmq]
            [langohr.channel :as lch]
            [langohr.basic :as lb]
            [langohr.consumers :as lc]
            [clojure.string :as string]
            [clojure.data.fressian :as fress]))
(comment
(let [conn (rmq/connect {:uri "amqp://localhost"})
      ch   (lch/open conn)]
  (lq/declare ch "test-queue")
  (try
    (if-let [[metadata msg] (lb/get ch "test-queue")]
      (println (fress/read msg)))
    (catch Exception e (.getMessage e))
    (finally (rmq/close ch))))

(let [conn (rmq/connect {:uri "amqp://localhost"})
      ch   (lch/open conn)]
  (lq/declare ch "test-queue")
  (lb/consume ch "test-queue"
    (lc/create-default ch
      :handle-delivery-fn (fn [ch metadata payload]
                            (println (fress/read payload))))))

(let [conn (rmq/connect {:uri "amqp://localhost"})
      ch   (lch/open conn)]
  (lq/declare ch "test-queue")
  (try
    (lb/publish
     ch "" "test-queue"
     (->> [{:a {:b [1 2 3] :c "CC" :d #{4 5 6}}} "A"]
          (fress/write)
          (.array)))
    (catch Exception e (.getMessage e))
    (finally (rmq/close ch)))))

(let [conn (rmq/connect {:uri "amqp://localhost"})
      ch   (lch/open conn)]
  (try
    (lb/publish
     ch "" "log-entry"
     (->> ["kawasima" (java.lang.System/currentTimeMillis) "192.168.0.1" 0]
          (string/join " " )
          (fress/write)
          (.array)))
    (catch Exception e (.getMessage e))
    (finally (rmq/close ch))))






