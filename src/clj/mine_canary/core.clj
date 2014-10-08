(ns mine-canary.core
  (:import [backtype.storm StormSubmitter LocalCluster])
  (:require [clojure.string :as string]
            [mine-canary.ulon-colon :as uc])
  (:use [backtype.storm clojure config])
  (:gen-class))

(defbolt split-log-entry ["time" "account" "ip-address" "success?"]
  [tuple collector]
  (let [entries (string/split (.getString tuple 0) #"\s+")]
    (emit-bolt! collector entries))
  (ack! collector tuple))

(defn in-the-period? [tm-vec]
  (println "in-the-period?" (count tm-vec) (- (first tm-vec) (last tm-vec)))
  (and (= (count tm-vec) 5)
       (< (- (first tm-vec) (last tm-vec)) 60000)))

;;; 一定時間内に同一ユーザの大量のログイン失敗を検出する
(defbolt failures-by-same-user ["account" "times"] {:prepare true}
  [conf context collector]
  (let [counts (atom {})]
    (bolt
     (execute [tuple]
       (let [[tm account ip-address success?] (.getValues tuple)
             tm (long (Double/parseDouble tm))
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
       (let [[tm account ip-address success?] (.getValues tuple)
             tm (long (Double/parseDouble tm))
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
   {"1" (spout-spec uc/access-log-spout)}
   {"2" (bolt-spec {"1" :shuffle}
                   split-log-entry
                   :p 3)
    "3" (bolt-spec {"2" ["account"]}
                   failures-by-same-user
                   :p 3)
    "4" (bolt-spec {"2" ["ip-address"]}
                   failures-by-same-ip
                   :p 3)
    "5" (bolt-spec {"3" :shuffle "4" :shuffle}
                   uc/push-to-control-panel
                   :p 1)}))

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


