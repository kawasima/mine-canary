(ns mine-canary.core
  (:import [backtype.storm StormSubmitter LocalCluster])
  (:require [langohr.queue :as lq]
            [langohr.core :as rmq]
            [langohr.channel :as lch]
            [langohr.basic :as lb]
            [langohr.consumers :as lc]
            [clojure.data.fressian :as fress]
            [clojure.string :as string])
  (:use [backtype.storm clojure config])
  (:gen-class))

(defspout access-log-spout ["log-entry"]
  [conf context collector]
  (let [conn (rmq/connect {:uri "amqp://localhost"})
        ch   (lch/open conn)]
    (lq/declare ch "log-entry")
    (spout
     (nextTuple
      []
      (let [[metadata payload] (lb/get ch "log-entry")]
        (if payload
          (emit-spout! collector [(fress/read payload)])
          (Thread/sleep 1000))))
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

(defn in-the-period? [tm-vec]
  (and (= (count tm-vec) 5)
       (< (- (first tm-vec) (last tm-vec)) 60000)))

;;; 一定時間内に同一ユーザの大量のログイン失敗を検出する
(defbolt failures-by-same-user ["account" "times"] {:prepare true}
  [conf context collector]
  (let [counts (atom {})]
    (bolt
     (execute [tuple]
       (let [[account tm ip-address success?] (.getValues tuple)
             tm (Long. tm)
             success? (= success? "1")]
         (when-not success?
           (let [failures-tm (get @counts account [])]
             (reset! counts
                   (assoc @counts account (->> (conj failures-tm tm)
                                               (sort >)
                                               (take 5)
                                               vec))))
           (when (in-the-period? (@counts account))
             (emit-bolt! collector [account (@counts account)] :anchor tuple)))
         (ack! collector tuple))))))

;;; 一定時間内に同一IPからの異なるユーザへの大量のログイン失敗を検出する
(defbolt failures-by-same-ip ["account" "times"] {:prepare true}
  [conf context collector]
  (let [counts (atom {})]
    (bolt
     (execute [tuple]
       (let [[account tm ip-address success?] (.getValues tuple)
             tm (Long. tm)
             success? (= success? "1")]
         (when-not success?
           (let [failures-tm (get @counts ip-address [])]
             (reset! counts
                   (assoc @counts ip-address (->> (conj failures-tm tm)
                                               (sort >)
                                               (take 5)
                                               vec))))
           (when (in-the-period? (@counts ip-address))
             (emit-bolt! collector [ip-address (@counts ip-address)] :anchor tuple)))
         (ack! collector tuple))))))

;;; 同一ユーザが距離の離れた場所からログイン成功を検出する

(defn mk-topology []

  (topology
   {"1" (spout-spec access-log-spout)}
   {"2" (bolt-spec {"1" :shuffle}
                   split-log-entry
                   :p 3)
    "3" (bolt-spec {"2" ["account"]}
                   failures-by-same-user
                   :p 3)
    "4" (bolt-spec {"2" ["ip-address"]}
                   failures-by-same-ip
                   :p 3)}))

(defn run-local! []
  (let [cluster (LocalCluster.)]
    (.submitTopology cluster "unauthorized-access" {TOPOLOGY-DEBUG true} (mk-topology))
    (.addShutdownHook (Runtime/getRuntime)
                      (Thread. (bound-fn []
                                 (.shutdown cluster))))))

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

