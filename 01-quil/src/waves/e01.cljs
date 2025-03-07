(ns waves.e01
  (:require [quil.core :as q]
            [quil.middleware :as m]))

(def width (.-innerWidth js/window) )
(def height (.-innerHeight js/window))

(defn setup []
  (q/frame-rate 30)
  (q/color-mode :hsb)
  {:time 0})

(defn update-state [state]
  (update state :time #(+ % 0.02)))

(defn draw-waves [time]
  (q/no-stroke)
  (doseq [y (range 0 height 10)]
    (doseq [x (range 0 width 10)]
      (let [offset (* 30 (q/noise (* x 0.01) (* y 0.01) time))
            wave-height (+ y offset)]
        (q/fill 200 255 (- 255 (* 0.7 wave-height)))
        (q/rect x wave-height 10 10)))))

(defn draw [state]
  (q/background 240 50 255)
  (draw-waves (:time state)))

(defn init []
(q/defsketch wave-sketch
             :title "Realistic Water Waves"
             :size [width height]
             :host "app"
             :setup setup
             :update update-state
             :draw draw
             :middleware [m/fun-mode]))