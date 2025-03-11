(ns fractals.e01
  (:require [quil.core :as q]
            [quil.middleware :as m]))


(defn setup []
  (q/frame-rate 10)
  (q/color-mode :hsb)
  {:shapes
   (repeatedly
     10
     #(vector (rand (q/width))
              (rand (q/height))
              (rand 255)
              (rand 100)
              (rand 100)))})

(defn draw [{:keys [shapes]}]
  (q/background 0)
  (doseq [[x y h s b] shapes]
    (q/fill h s b)
    (q/ellipse x y (rand 100) (rand 100))))

(defn init []
  (q/defsketch
    abstract-art
    :host "app"
    :size [800 600]
    :setup setup
    :draw draw
    :middleware [m/fun-mode]))

