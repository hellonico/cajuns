(ns example.events
  (:require
   [re-frame.core :as rf]
   ;["react-native" :refer [ReactNative]]
   [example.db :as db :refer [app-db]]))


(rf/reg-event-db
 :initialize-db
 (fn [_ _]
   app-db))

(rf/reg-event-db
 :show-versions
 (fn [_ _]
  (js/require "react-native")
  ;(js/console.log "React version:" (.-version js/React))
  ;(js/console.log "React Native version:" (.. js/ReactNative -version))
  app-db))

(rf/dispatch-sync [:initialize-db])

(rf/reg-event-db
 :inc-counter
 (fn [db [_ _]]
   (update db :counter inc)))

(rf/reg-event-db
 :dec-counter
 (fn [db [_ _]]
  (update db :counter dec)))

(rf/reg-event-db
 :navigation/set-root-state
 (fn [db [_ navigation-root-state]]
   (assoc-in db [:navigation :root-state] navigation-root-state)))
