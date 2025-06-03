(ns abstract-art.e03
 (:require [threeagent.core :as th]))

(defonce state (th/atom {:ticks 0}))

(def colors ["red" "blue" "green" "yellow" "purple" "orange"])

(def cube-count 10)

(defn random-position []
 [(+ -5 (rand 10)) (+ 1 (rand 4)) (+ -8 (rand 6))])

(defn random-speed []
 (+ 0.5 (rand 2)))

(def cubes
 (map (fn [color pos speed]
       {:color color :position pos :speed speed})
      (take cube-count (cycle colors))
      (repeatedly cube-count random-position)
      (repeatedly cube-count random-speed)))

(defn floating-cube [{:keys [color position speed]}]
 [:object {:position [(+ (first position) (* speed (.sin js/Math (/ (:ticks @state) 100))))
                      (second position)
                      (last position)]
           :rotation [(* speed (.sin js/Math (/ (:ticks @state) 50)))
                      (* speed (.cos js/Math (/ (:ticks @state) 70)))
                      (* speed (.sin js/Math (/ (:ticks @state) 90))) ]}
  [:box {:dims [2 2 2]
         :receive-shadow true
         :cast-shadow true
         :material {:color color}}]])

(defn root []
 [:object
  [:ambient-light {:intensity 0.5}]
  [:object {:rotation [0 0 0]
            :position [0 0 0]}
   [:point-light {:intensity   1.0
                  :position    [2 10 2]
                  :cast-shadow true}]]
  [:object {:position [1.0 0 -8.0]}
   [:plane {:scale [200 200 10]
            :receive-shadow true}]]
  (for [cube cubes]
   ^{:key (:color cube)} [floating-cube cube])])

(defn ^:dev/after-load reload []
 (js/console.log
  (th/render root
             (.getElementById js/document "app")
             ;(.-body js/document)
             {:shadow-map {:enabled true}})))

(defn init []
 (.setInterval js/window #(swap! state update :ticks inc) 10)
 (reload))
