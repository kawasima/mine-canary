(ns mine-canary.core
  (:import [backtype.storm StormSubmitter LocalCluster])
  (:require [clj-kafka.consumer.zk :as kafka-consumer])
  (:use [backtype.storm clojure config]
        [clj-kafka.core])
  (:gen-class))

(defspout access-log-spout ["account" "ip-address"]
  [conf context collector]
  (let [consumer (kafka-consumer/consumer {"zookeeper.connect" "localhost:2181"
                                           "group.id" "mine-canary.consumer"
                                           "auto.offset.reset" "smallest"
                                           "auto.commit.enable" "false"})]
    (spout
     (nextTuple
      []
      (doseq [msg (take 5 (kafka-consumer/messages consumer ["log-entry"]))]
        (emit-spout! collector msg)))
     (ack [id]
        ;; You only need to define this method for reliable spouts
        ;; (such as one that reads off of a queue like Kestrel)
        ;; This is an unreliable spout, so it does nothing here
        )
     (close
      []
      (kafka-consumer/shutdown consumer)))))

(defbolt split-log-entry ["account" "time" "ip-address" "success?"]
  [tuple collector]
  (let [entries (.split (.gfetString tuple 0) " ")]
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



