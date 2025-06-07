(ns waves.e03
  (:require [quil.core :as q]
            [quil.middleware :as m]))

(def width (.-innerWidth js/window))
(def height (.-innerHeight js/window))

(def cols 60) ;; Number of wave segments
(def wave-height 50) ;; Increased wave height for larger space

(defn setup []
  (q/frame-rate 30)
  (q/color-mode :hsb)
  {:time 0})

(defn update-state [state]
  (update state :time #(+ % 0.02))) ;; Slightly faster waves

(defn wave-y [x time]
  (+ (/ height 2) (* wave-height (q/noise (* x 0.02) time))))


(defn draw-wave [time]
  (doseq [i (range 5)] ;; More layers for a richer gradient effect
    (let [alpha (max 120 (- 255 (* 40 i))) ;; Adjust alpha for more visible waves
          y-offset (* i 20)] ;; Increased layer offset
      (let [fill-color (q/color (+ 180 (* 5 i)) 150 alpha)] ;; Stronger gradient effect
        (q/fill fill-color)
        (q/no-stroke)
        (q/begin-shape)
        (q/vertex 0 height) ;; Start from bottom
        (doseq [x (range 0 width (/ width cols))]
          (let [y (- (wave-y x (+ time (* i 0.1))) y-offset)]
            (q/vertex x y)))
        (q/vertex width height) ;; End at bottom
        (q/end-shape)))))


(defn draw [state]
  (q/background 180 90 255) ;; Light sky-blue background for more contrast
  (draw-wave (:time state)))

(defn init []
  (q/defsketch wave-sketch
               :title "Realistic Water Waves"
               :size [width height]
               :host "app"
               :setup setup
               :update update-state
               :draw draw
               :middleware [m/fun-mode]))