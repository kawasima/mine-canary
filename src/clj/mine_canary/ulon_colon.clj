(ns mine-canary.ulon-colon
  (:import [backtype.storm StormSubmitter LocalCluster])
  (:require [clojure.string :as string])
  (:use [backtype.storm clojure config]
        [ulon-colon.consumer]
        [ulon-colon.producer]))

(defspout access-log-spout ["log-entry"]
  [conf context collector]
  (let [consumer (make-consumer "ws://localhost:5629")]
    (spout
     (nextTuple
      []
      (consume-sync consumer
                    (fn [msg]
                      (emit-spout! collector [msg]))))
     (ack [id]
          ;; You only need to define this method for reliable spouts
          ;; (such as one that reads off of a queue like Kestrel)
          ;; This is an unreliable spout, so it does nothing here
          )
     (close []))))

(defbolt push-to-control-panel ["account" "times"] {:prepare true}
  [conf context collector]
  (let [counts (atom {})
        procuder (start-producer :port 56293)]
    (bolt
     (execute [tuple]
       (let [[account times] (.getValues tuple)]
         (produce {:account account :times times}))))))
