(ns abstract-art.e04
 (:require [threeagent.core :as th]))

(defonce state (th/atom {:ticks 0}))

(def colors ["red" "blue" "green" "yellow" "purple" "orange"])

(def cube-count 10)

(defn random-position []
 [(+ -50 (rand 100))  ;; X: between -50 and 50
  (+ 10 (rand 40))    ;; Y: between 10 and 50
  (+ -50 (rand 100))])  ;; Z: between -50 and 50

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
  [:box {:dims [150 150 150]  ;; Adjusted size
         :receive-shadow true
         :cast-shadow true
         :material {:color color}}]])

(defn root []
 [:object
  [:ambient-light {:intensity 0.5}]
  [:object {:rotation [0 0 0]
            :position [0 0 0]}
   [:point-light {:intensity 1.0
                  :position [0 100 0]
                  :cast-shadow true}]]
  [:object {:position [0 0 -50]}
   [:plane {:scale [200 200 1]
            :receive-shadow true}]]
  (for [cube cubes]
   ^{:key (:color cube)} [floating-cube cube])])

(defn ^:dev/after-load reload []
 (js/console.log
  (th/render root (.-body js/document)
             {:shadow-map {:enabled true}
              :camera {:position [0 100 300]  ;; Adjusted camera position
                       :fov 50               ;; Adjusted FOV
                       :near 1               ;; Near clipping plane
                       :far 1000}})))        ;; Far clipping plane

(defn init []
 (.setInterval js/window #(swap! state update :ticks inc) 10)
 (reload))