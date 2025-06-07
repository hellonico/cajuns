(ns bubbles.e01
 (:require [quil.core :as q]
           [quil.middleware :as m]))


(defonce bubbles (atom []))
(defonce mouse-pos (atom {:x 0 :y 0}))

(defn random-gradient []
 "Generate a smooth gradient with subtle color variation."
 (let [r (rand-int 150)
       g (rand-int 150)
       b (rand-int 150)]
  {:color1 [r g b 255]
   :color2 [(+ r (rand-int 80)) (+ g (rand-int 80)) (+ b (rand-int 80)) 50]}))

(defn create-bubble [x y size]
 "Create a bubble with random properties."
 {:x x
  :y y
  :size size
  :base-speed (- 0.5 (rand 1.0)) ;; Base speed
  :speed 0
  :gradient (random-gradient)})

(defn setup []
 "Initialize the canvas and create initial bubbles."
 (q/frame-rate 30)
 (q/background 20)
 (reset! bubbles
         (for [_ (range 8)]
          (create-bubble (rand (q/width)) (rand (q/height)) (+ 60 (rand 80))))))

(defn draw-gradient [x y size gradient]
 "Smoothly draw a bubble with a radial gradient."
 (let [{[r1 g1 b1 a1] :color1
        [r2 g2 b2 a2] :color2} gradient]
  (doseq [i (range 0 size 1)]
   (let [t (/ i size)]
    (q/fill (q/lerp r1 r2 t)
            (q/lerp g1 g2 t)
            (q/lerp b1 b2 t)
            (q/lerp a1 a2 t))
    (q/no-stroke)
    (q/ellipse x y (- size i) (- size i))))))

(defn update-bubbles []
 "Move bubbles upward and apply speed boost near the mouse."
 (swap! bubbles
        (fn [bs]
         (for [b bs]
          (let [{:keys [x y size base-speed]} b
                {:keys [x mx y my]} @mouse-pos
                dist (q/dist x y mx my)
                boost (if (< dist 150) (* (- 1 (/ dist 150)) 4.0) 0)] ;; Speed boost near cursor
           (assoc b
            :y (+ y (+ base-speed boost))
            :speed (+ base-speed boost)))))))

(defn draw []
 "Render bubbles and update their positions."
 (q/background 20)
 (doseq [{:keys [x y size gradient]} @bubbles]
  (draw-gradient x y size gradient))
 (update-bubbles))

(defn split-bubble [b]
 "Split a bubble into two smaller ones with the same gradient."
 (let [{:keys [x y size gradient]} b
       new-size (/ size 1.6)]
  [(create-bubble (- x (/ size 3)) y new-size)
   (create-bubble (+ x (/ size 3)) y new-size)]))

(defn mouse-pressed []
 "Split a bubble when clicked."
 (swap! bubbles
        (fn [bs]
         (reduce (fn [new-bs b]
                  (if (< (q/dist (:x b) (:y b) (q/mouse-x) (q/mouse-y)) (:size b))
                   (into new-bs (split-bubble b))
                   (conj new-bs b)))
                 []
                 bs))))

(defn mouse-moved []
 "Update mouse position."
 (reset! mouse-pos {:x (q/mouse-x) :y (q/mouse-y)}))

;(defn window-resized []
; "Resize canvas when window size changes."
; (q/resize-canvas (q/window-width) (q/window-height)))

(defn init []
 (q/defsketch floating-bubbles
              ;:size [600 600]
              :size [(.-innerWidth js/window) (.-innerHeight js/window)]
              :host "app"
              :setup setup
              :draw draw
              :mouse-moved mouse-moved
              :mouse-pressed mouse-pressed
              :middleware [m/fun-mode]))