(ns death-star.e02
 (:require [goog.dom :as gdom]))

;; --- Configurable parameters ---
(def rings 30)
(def points-per-ring 40)
(def fov 300)
(def speed 10)
(def smooth-factor 0.1)    ;; smoothing for turn interpolation
(def max-angle 0.5)       ;; max radians for yaw/pitch

;; --- Canvas setup ---
(def canvas (gdom/getElement "canvas"))
(def ctx    (.getContext canvas "2d"))

;; Atoms to track smoothed yaw/pitch and target yaw/pitch
(def angles (atom {:yaw 0 :pitch 0}))
(def target-angles (atom {:yaw 0 :pitch 0}))

(defn set-size! []
      (let [w (.-innerWidth js/window)
            h (.-innerHeight js/window)]
           (aset canvas "width"  w)
           (aset canvas "height" h)))

;; initialize size and make responsive
(set-size!)
(.addEventListener js/window "resize" set-size!)

;; update target yaw/pitch on mousemove
(.addEventListener canvas "mousemove"
                   (fn [e]
                       (let [rect (.getBoundingClientRect canvas)
                             mx   (- (.-clientX e) (.-left rect))
                             my   (- (.-clientY e) (.-top rect))
                             w    (.-width canvas)
                             h    (.-height canvas)
                             ;; map mouse pos to [-1,1]
                             nx   (/ (- mx (/ w 2)) (/ w 2))
                             ny   (/ (- my (/ h 2)) (/ h 2))
                             ty   (* max-angle nx)
                             tp   (* max-angle (- ny))]
                            (reset! target-angles {:yaw ty :pitch tp}))))

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

;; --- Projection with turning ---
(defn project [{:keys [x y z]}]
      ;; interpolate angles
      (swap! angles (fn [{:keys [yaw pitch]}]
                        (let [{ty :yaw tp :pitch} @target-angles
                              dy  (- ty yaw)
                              dp  (- tp pitch)]
                             {:yaw   (+ yaw (* smooth-factor dy))
                              :pitch (+ pitch (* smooth-factor dp))})))
      (let [{yaw :yaw pitch :pitch} @angles
            ;; rotate point around Y (yaw) and X (pitch)
            ;; yaw: x-z plane
            x1   (+ (* x (Math/cos yaw)) (* z (Math/sin yaw)))
            z1   (- (* z (Math/cos yaw)) (* x (Math/sin yaw)))
            ;; pitch: y-z plane
            y1   (+ (* y (Math/cos pitch)) (* z1 (Math/sin pitch)))
            z2   (- (* z1 (Math/cos pitch)) (* y (Math/sin pitch)))
            ;; perspective proj
            d    (max 1 (- z2 speed))
            scale (/ fov d)
            cx   (/ (.-width  canvas) 2)
            cy   (/ (.-height canvas) 2)]
           [(+ cx (* x1 scale))
            (+ cy (* y1 scale))]))

;; --- Update points ---
(defn update-point [{:keys [z] :as pt}]
      (let [z2 (- z speed)
            z3 (if (< z2 speed)
                (* rings 200)
                z2)]
           (assoc pt :z z3)))

;; --- Draw loop ---
(defn draw-frame []
      ;; advance points
      (swap! points (fn [pts] (mapv update-point pts)))

      ;; clear
      (doto ctx
            (aset "fillStyle" "rgba(0,0,0,0.2)")
            (.fillRect 0 0 (.-width canvas) (.-height canvas))
            (aset "strokeStyle" "#00ff00")
            (aset "lineWidth" 1))

      ;; draw spokes
      (.beginPath ctx)
      (doseq [r (range (dec rings))
              i (range points-per-ring)]
             (let [[x1 y1] (project (nth @points (+ (* r points-per-ring) i)))
                   [x2 y2] (project (nth @points (+ (* (inc r) points-per-ring) i)))]
                  (.moveTo ctx x1 y1)
                  (.lineTo ctx x2 y2)))
      (.stroke ctx)

      ;; draw rings
      (.beginPath ctx)
      (doseq [r (range rings)
              i (range points-per-ring)]
             (let [[x1 y1] (project (nth @points (+ (* r points-per-ring) i)))
                   [x2 y2] (project (nth @points (+ (* r points-per-ring) (mod (inc i) points-per-ring))))]
                  (.moveTo ctx x1 y1)
                  (.lineTo ctx x2 y2)))
      (.stroke ctx)

      ;; next frame
      (js/requestAnimationFrame draw-frame))

(defn ^:export init []
      (draw-frame))

;; start
;(init)