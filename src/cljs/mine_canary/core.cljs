(ns mine-canary.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:use [ulon-colon.consumer :only [make-consumer consume consume-sync]])
  (:require [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :refer [put! <! chan]]
            [goog.events :as events]))

(enable-console-print!)

(defcomponent main-app [app owner]
  (will-mount
   [_]
   (let [consumer (make-consumer "ws://localhost:56293")]
     (consume consumer
              (fn [msg]
                (om/transact! :alerts #(conj % msg))))))
  (render
   [_]
   (html
    [:table
     [:tr
      [:th "Account"]
      [:th "Trial times"]]
     (for [{:keys [account times]} alerts]
       [:tr
        [:td account]
        [:td times]])])))

(om/root main-app {:alerts []}
         {:target (.getElementById js/document "app")})

