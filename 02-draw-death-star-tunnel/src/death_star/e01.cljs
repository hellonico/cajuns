(ns death-star.e01
 (:require [goog.dom :as gdom]))

;; --- Configurable parameters ---
(def rings 30)
(def points-per-ring 40)
(def fov 300)
(def speed 10)

;; --- Canvas setup ---
(def canvas (gdom/getElement "canvas"))
(def ctx    (.getContext canvas "2d"))

;; Atom to track the dynamic center (vanishing point)
(def center (atom {:x 0 :y 0}))

(defn set-size! []
      (let [w (.-innerWidth js/window)
            h (.-innerHeight js/window)]
           (aset canvas "width"  w)
           (aset canvas "height" h)
           ;; reset center to middle
           (reset! center {:x (/ w 2)
                           :y (/ h 2)})))

;; initialize size and make responsive
(set-size!)
(.addEventListener js/window "resize" set-size!)

;; mouse movement to update vanishing-point
(.addEventListener canvas "mousemove"
                   (fn [e]
                       (let [rect (.getBoundingClientRect canvas)
                             x    (- (.-clientX e) (.-left rect))
                             y    (- (.-clientY e) (.-top rect))]
                            (reset! center {:x x :y y}))))

;; --- 3D Point generation ---
(defn make-point [ring idx]
      (let [angle  (* 2 Math/PI (/ idx points-per-ring))
            radius (+ 50 (* ring 15))]
           {:x (* (Math/cos angle) radius)
            :y (* (Math/sin angle) radius)
            :z (* ring 200)}))

(def points
 (atom (vec (for [r (range rings)
                  i (range points-per-ring)]
                 (make-point r i)))))

;; --- Projection ---
(defn project [{:keys [x y z]}]
      (let [d     (max 1 (- z speed))
            scale (/ fov d)
            cx    (:x @center)
            cy    (:y @center)]
           [(+ cx (* x scale))
            (+ cy (* y scale))]))

;; --- Update points ---
(defn update-point [{:keys [z] :as pt}]
      (let [z2 (- z speed)
            z3 (if (< z2 speed)
                (* rings 200)
                z2)]
           (assoc pt :z z3)))

;; --- Draw loop ---
(defn draw-frame []
      ;; move points forward
      (swap! points (fn [pts] (mapv update-point pts)))

      ;; clear with semi-transparent black
      (doto ctx
            (aset "fillStyle"   "rgba(0, 0, 0, 0.2)")
            (.fillRect 0 0 (.-width canvas) (.-height canvas))
            (aset "strokeStyle" "#00ff00")
            (aset "lineWidth"   1))

      ;; draw spokes
      (.beginPath ctx)
      (doseq [r (range (dec rings))
              i (range points-per-ring)]
             (let [idx1 (+ (* r points-per-ring) i)
                   idx2 (+ (* (inc r) points-per-ring) i)
                   [x1 y1] (project (nth @points idx1))
                   [x2 y2] (project (nth @points idx2))]
                  (.moveTo ctx x1 y1)
                  (.lineTo ctx x2 y2)))
      (.stroke ctx)

      ;; draw rings
      (.beginPath ctx)
      (doseq [r (range rings)
              i (range points-per-ring)]
             (let [idx1 (+ (* r points-per-ring) i)
                   idx2 (+ (* r points-per-ring) (mod (inc i) points-per-ring))
                   [x1 y1] (project (nth @points idx1))
                   [x2 y2] (project (nth @points idx2))]
                  (.moveTo ctx x1 y1)
                  (.lineTo ctx x2 y2)))
      (.stroke ctx)

      ;; loop
      (js/requestAnimationFrame draw-frame))

(defn ^:export init []
      (draw-frame))

;; auto-start
;(init)
