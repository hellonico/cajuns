(ns art.e02
  (:require [quil.core :as q]
            [quil.middleware :as m]))

(defn setup []
  (q/frame-rate 30)
  (q/color-mode :hsb)
  {:shapes (repeatedly 30 #(hash-map :x (rand (q/width))
                                     :y (rand (q/height))
                                     :size (+ 100 (rand 150)) ;; Fixed size range
                                     :hue (rand 255)
                                     :dx (- (rand 4) 2)
                                     :dy (- (rand 4) 2)))})

(defn update-state [state]
  (update
    state
    :shapes
    (fn [shapes]
      (map (fn [{:keys [x y size hue dx dy] :as shape}]
             (let [new-x (mod (+ x dx) (q/width))
                   new-y (mod (+ y dy) (q/height))]
               (assoc shape :x new-x :y new-y :hue (mod (+ hue 1) 255))))
           shapes))))

(defn draw [{:keys [shapes]}]
  (q/background 255)
  (doseq [{:keys [x y size hue]} shapes]
    (q/fill hue 100 100)
    (q/ellipse x y size size)))

(defn init []
  (q/defsketch abstract-art
               :host "app"
               ;:size [800 600]
               :size [(.-innerWidth js/window) (.-innerHeight js/window)]
               ;:size [(q/display-width) (q/display-height)]
               :setup setup
               :update update-state
               :draw draw
               :middleware [m/fun-mode]))
