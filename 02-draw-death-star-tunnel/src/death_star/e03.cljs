(ns death-star.e03
 (:require [goog.dom :as gdom]))

;; --- Configurable parameters ---
(def rings 30)
(def points-per-ring 40)
(def fov 300)
(def spacing 200)
(def smooth-factor 0.1)
(def max-angle 0.5)

;; dynamic speed
(def speed (atom 10))

;; --- Canvas setup ---
(def canvas (gdom/getElement "canvas"))
(def ctx    (.getContext canvas "2d"))

(defn set-size! []
      (let [w (.-innerWidth js/window)
            h (.-innerHeight js/window)]
           (aset canvas "width"  w)
           (aset canvas "height" h)))
(set-size!)
(.addEventListener js/window "resize" set-size!)

;; --- Steering state ---
(def steering-target (atom {:yaw 0 :pitch 0}))
(def steering        (atom {:yaw 0 :pitch 0}))

(.addEventListener canvas "mousemove"
                   (fn [e]
                       (let [rect (.getBoundingClientRect canvas)
                             mx   (- (.-clientX e) (.-left rect))
                             my   (- (.-clientY e) (.-top rect))
                             w    (.-width canvas)
                             h    (.-height canvas)
                             nx   (/ (- mx (/ w 2)) (/ w 2))
                             ny   (/ (- my (/ h 2)) (/ h 2))]
                            (reset! steering-target
                                    {:yaw   (* max-angle nx)
                                     :pitch (* max-angle (- ny))}))))

;; --- Speed controls: Up/Down arrows ---
(.addEventListener js/window "keydown"
                   (fn [e]
                       (case (.-keyCode e)
                             38 (swap! speed inc)            ;; up arrow: increase
                             40 (swap! speed (fn [s] (max 1 (dec s))))  ;; down arrow: decrease
                             nil)))

;; --- Pipe cross-section radius (constant) ---
(def cross-radius 50)

;; --- Pipe centers initial positions ---
(def max-z (* (dec rings) spacing))
(def centers
 (atom (vec (for [i (range rings)]
                 {:x 0 :y 0 :z (* i spacing)}))))

;; --- Update each center: move, clamp to cross-section, wrap z ---
(defn update-center [{:keys [x y z]}]
      (let [{:keys [yaw pitch]} @steering
            spd              @speed
            ;; move center
            x2 (+ x (* yaw spd))
            y2 (+ y (* pitch spd))
            ;; clamp to circular cross-section: radius = cross-radius
            dist (Math/sqrt (+ (* x2 x2) (* y2 y2)))
            factor (if (> dist cross-radius) (/ cross-radius dist) 1)
            x3 (* x2 factor)
            y3 (* y2 factor)
            ;; advance z and wrap
            z2 (- z spd)
            z3 (if (< z2 0) max-z z2)]
           {:x x3 :y y3 :z z3}))

;; --- 3D Projection ---
(defn project [{:keys [x y z]}]
      (let [d     (max 1 z)
            scale (/ fov d)
            cx    (/ (.-width  canvas) 2)
            cy    (/ (.-height canvas) 2)]
           [(+ cx (* x scale))
            (+ cy (* y scale))]))

;; --- Animation Loop ---
(defn draw-frame []
      ;; smooth steering interpolation
      (swap! steering
             (fn [{:keys [yaw pitch]}]
                 (let [{ty :yaw tp :pitch} @steering-target]
                      {:yaw   (+ yaw (* smooth-factor (- ty yaw)))
                       :pitch (+ pitch (* smooth-factor (- tp pitch)))})))

      ;; update all centers
      (swap! centers (fn [cs] (mapv update-center cs)))

      ;; clear canvas with trailing effect
      (doto ctx
            (aset "fillStyle"   "rgba(0,0,0,0.2)")
            (.fillRect 0 0 (.-width canvas) (.-height canvas))
            (aset "strokeStyle" "#00ff00")
            (aset "lineWidth"   1))

      ;; draw vertical lines (spokes)
      (.beginPath ctx)
      (doseq [r (range (dec rings))
              i (range points-per-ring)]
             (let [c1    (nth @centers r)
                   c2    (nth @centers (inc r))
                   angle (* 2 Math/PI (/ i points-per-ring))
                   rad   (+ cross-radius (* r 15))
                   p1    {:x (+ (:x c1) (* (Math/cos angle) rad))
                          :y (+ (:y c1) (* (Math/sin angle) rad))
                          :z (:z c1)}
                   p2    {:x (+ (:x c2) (* (Math/cos angle) rad))
                          :y (+ (:y c2) (* (Math/sin angle) rad))
                          :z (:z c2)}
                   [x1 y1] (project p1)
                   [x2 y2] (project p2)]
                  (.moveTo ctx x1 y1)
                  (.lineTo ctx x2 y2)))
      (.stroke ctx)

      ;; draw horizontal rings
      (.beginPath ctx)
      (doseq [r (range rings)
              i (range points-per-ring)]
             (let [c     (nth @centers r)
                   angle1 (* 2 Math/PI (/ i points-per-ring))
                   angle2 (* 2 Math/PI (/ (mod (inc i) points-per-ring) points-per-ring))
                   rad    (+ cross-radius (* r 15))
                   p1     {:x (+ (:x c) (* (Math/cos angle1) rad))
                           :y (+ (:y c) (* (Math/sin angle1) rad))
                           :z (:z c)}
                   p2     {:x (+ (:x c) (* (Math/cos angle2) rad))
                           :y (+ (:y c) (* (Math/sin angle2) rad))
                           :z (:z c)}
                   [x1 y1] (project p1)
                   [x2 y2] (project p2)]
                  (.moveTo ctx x1 y1)
                  (.lineTo ctx x2 y2)))
      (.stroke ctx)

      ;; schedule next frame
      (js/requestAnimationFrame draw-frame))

(defn ^:export init []
      (draw-frame))

;(init)
