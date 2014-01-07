(ns mine-canary.core
  (:import [backtype.storm StormSubmitter LocalCluster])
  (:require [langohr.queue :as lq]
            [langohr.core :as rmq]
            [langohr.channel :as lch]
            [langohr.basic :as lb]
            [langohr.consumers :as lc]
            [clojure.string :as string])
  (:use [backtype.storm clojure config])
  (:gen-class))

(defspout access-log-spout ["log-entry"]
  [conf context collector]
  (let [conn (rmq/connect {:uri "amqp://172.17.0.9:5672"})
        ch   (lch/open conn)]
    (spout
     (nextTuple
      []
      (let [[metadata payload] (lb/get ch "log-entry")]
        (emit-spout! collector [payload])))
     (ack [id]
          ;; You only need to define this method for reliable spouts
          ;; (such as one that reads off of a queue like Kestrel)
          ;; This is an unreliable spout, so it does nothing here
          )
     (close []
            (rmq/close ch)))))

(defbolt split-log-entry ["account" "time" "ip-address" "success?"]
  [tuple collector]
  (let [entries (string/split (.getString tuple 0) #"\s+")]
    (emit-bolt! collector entries))
  (ack! collector tuple))

(defbolt failures-in-given-period ["account" "time"] {:prepare true}
  [conf context collector]
  (let [counts (atom {})]
    (bolt
     (execute [tuple]
       (let [account (.getString tuple 0)]
         (swap! counts (partial merge-with +) {account 1})
         (emit-bolt! collector [account (@counts account)] :anchor tuple)
         (ack! collector tuple)
         )))))

(defn mk-topology []

  (topology
   {"1" (spout-spec access-log-spout)}
   {"2" (bolt-spec {"1" :shuffle}
                   split-log-entry
                   :p 3)
    "3" (bolt-spec {"2" ["account"]}
                   failures-in-given-period
                   :p 5)}))

(defn run-local! []
  (let [cluster (LocalCluster.)]
    (.submitTopology cluster "unauthorized-access" {TOPOLOGY-DEBUG true} (mk-topology))
    ;;(Thread/sleep 10000)
    ;;(.shutdown cluster)
    ))

(defn submit-topology! [name]
  (StormSubmitter/submitTopology
   name
   {TOPOLOGY-DEBUG true
    TOPOLOGY-WORKERS 3}
   (mk-topology)))

(defn -main
  ([]
   (run-local!))
  ([name]
   (submit-topology! name)))







