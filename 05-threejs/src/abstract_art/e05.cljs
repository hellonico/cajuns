(ns abstract-art.e05
 (:require [threeagent.core :as th]))


(defonce state (th/atom {:ticks    0
                         :camera-z 100                      ;; Camera Z position for zooming
                         :camera-x 0                        ;; Camera X position for panning
                         :camera-y 100}))                   ;; Camera Y position for panning

(def colors ["brown" "lightgreen" "red" "red" "blue" "cyan" "magenta" "green" "yellow" "purple" "orange"])

(def cube-count 200)

(defn random-position []
 [(+ -5 (rand 10))                                        ;; X: between -50 and 50
  (+ -15 (rand 20))                                          ;; Y: between 10 and 50
  (+ -20 (rand 30))
  ;(rand 10)
  ])                                      ;; Z: between -50 and 50

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
                      (* speed (.sin js/Math (/ (:ticks @state) 90)))]}
  [:box {;:dims           [7 7 7]                         ;; Adjusted size
         :receive-shadow true
         :dims           [170 170 70]
         ;:cast-shadow    true
         :material       {:color color}}]])

(defn root []
 [:object
  [:ambient-light {:intensity 0.5}]
  [:object {:rotation [0 0 0]
            :position [0 0 0]}
   [:point-light {:intensity   3.0
                  :position    [0 100 0]
                  :cast-shadow true}]]
  [:object {:position [10 10 -50]}
   [:plane {:scale          [10 10 -1]
            :receive-shadow true}]]
  (for [cube cubes]
   ;^{:key (:color cube)}
   [floating-cube cube])])

(defn ^:dev/after-load reload []
 (js/console.log
  (th/render root (.-body js/document)
             {:shadow-map {:enabled true}
              :camera     {:position [(:camera-x @state) (:camera-y @state) (:camera-z @state)] ;; Use camera-x, camera-y, camera-z from state
                           :fov      50
                           :near     20
                           :far      100}})))

(defn ^:export init []
 ;; Add wheel event listener for zooming
 (.addEventListener js/window "wheel"
                    (fn [event]
                     (let [delta-y (.-deltaY event)         ;; Get scroll amount
                           zoom-speed 10]                   ;; Adjust zoom speed
                      (if (< delta-y 0)
                       ;; Zoom in (scroll up)
                       (swap! state update :camera-z #(- % zoom-speed))
                       ;; Zoom out (scroll down)
                       (swap! state update :camera-z #(+ % zoom-speed))))))

 ;; Add mousemove event listener for panning
 (.addEventListener js/window "mousemove"
                    (fn [event]
                     (let [movement-x (.-movementX event)   ;; Get horizontal mouse movement
                           movement-y (.-movementY event)   ;; Get vertical mouse movement
                           pan-speed 0.1]                   ;; Adjust panning speed
                      ;; Update camera X and Y position based on mouse movement
                      (swap! state update :camera-x #(- % (* movement-x pan-speed)))
                      (swap! state update :camera-y #(+ % (* movement-y pan-speed))))))

 ;; Start the tick interval
 (.setInterval js/window #(swap! state update :ticks inc) 10)
 (reload))
