(ns fractals.e02
  (:require [quil.core :as q]
            [quil.middleware :as m]))

(defn branch [x y len angle depth hue-offset]
  (when (> depth 0)
    (let [x2 (+ x (* len (q/cos angle)))
          y2 (+ y (* len (q/sin angle)))
          hue (mod (+ hue-offset (* 15 depth)) 255)]
      (q/stroke hue 255 255)
      (q/line x y x2 y2)
      (branch x2 y2 (* len 0.7) (+ angle 0.3) (dec depth) hue-offset)
      (branch x2 y2 (* len 0.7) (- angle 0.3) (dec depth) hue-offset))))

(defn setup []
  (q/color-mode :hsb 255)
  {:angle      0
   :hue-offset 0})

(defn update-state [state]
  (-> state
      (update :angle #(mod (+ % 0.02) q/TWO-PI))
      (update :hue-offset #(mod (+ % 2) 255))))

(defn draw [state]
  (q/background 0)
  (q/translate (/ (q/width) 2) (q/height))
  (branch 0 0 120 (- q/HALF-PI) 10 (:hue-offset state)))

(defn init []
  (q/defsketch
    fractal
    :host "app"
    :size [500 500]
    :setup setup
    :update update-state
    :draw draw
    :middleware [m/fun-mode]))
