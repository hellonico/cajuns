(ns hyperspace.core
 (:require
  [goog.dom :as gdom]
  [goog.events :as events]
  [goog.events.EventType :as EventType]))

(defonce ctx           (atom nil))
(defonce width         (atom 0))
(defonce height        (atom 0))
(defonce streak-stars  (atom []))
(defonce static-stars  (atom []))

(defn random-range
 "Return a random float between lo and hi."
 [lo hi]
 (+ lo (* (rand) (- hi lo))))

;; -------------------------------------------------------------------
;; 1) INITIALIZATION: CANVAS + CONTEXT + STAR ARRAYS
;; -------------------------------------------------------------------

(defn init-canvas-size!
 "Resize the <canvas> to match the window’s inner dimensions."
 []
 (let [canvas (gdom/getElement "hyperspace-canvas")
       w      (.-innerWidth js/window)
       h      (.-innerHeight js/window)]
  (reset! width w)
  (reset! height h)
  (set! (.-width  canvas) w)
  (set! (.-height canvas) h)))

(defn init-context!
 "Grab the 2D rendering context from the already-sized <canvas>."
 []
 (let [canvas (gdom/getElement "hyperspace-canvas")]
  (reset! ctx (.getContext canvas "2d"))))

(defn make-streak-star
 "Create one streaking star at (center) with:
  - random direction,
  - initial radius near zero,
  - speed & accel for movement,
  - a :blue? flag (≈20% chance) so some streaks glow blue."
 []
 (let [angle   (random-range 0 (* 2 Math/PI))
       radius  (random-range 0.5 2.0)
       ;; increase speed range for extra velocity
       speed   (random-range 0.1 0.3)
       ;; increase acceleration slightly
       accel   (random-range 0.0002 0.0005)
       ;; ~20% of streaks will be blue
       blue?   (< (rand) 0.2)]
  {:angle  angle
   :radius radius
   :speed  speed
   :accel  accel
   :blue?  blue?}))

(defn init-streak-stars!
 "Populate `streak-stars` with N fast-moving star maps."
 [N]
 (reset! streak-stars (vec (repeatedly N make-streak-star))))

(defn make-static-star
 "Generate one static background star at a random x,y (across full canvas),
  with a small size and low brightness."
 []
 {:x          (random-range 0 @width)
  :y          (random-range 0 @height)
  :size       (random-range 0.3 1.2)
  :brightness (random-range 0.1 0.3)})

(defn init-static-stars!
 "Populate `static-stars` with M stationary stars."
 [M]
 (reset! static-stars (vec (repeatedly M make-static-star))))

;; -------------------------------------------------------------------
;; 2) DRAWING FUNCTIONS
;; -------------------------------------------------------------------

(defn draw-static-stars!
 "Draw each static star as a tiny circle with low alpha so they appear in the background."
 []
 (let [ctx2 @ctx]
  (when ctx2
   ;; No shadow for static stars
   (set! (.-shadowBlur   ctx2) 0)
   (set! (.-shadowColor  ctx2) "transparent")
   (doseq [{:keys [x y size brightness]} @static-stars]
    (set! (.-globalAlpha ctx2) brightness)
    (set! (.-fillStyle ctx2)   "#ffffff")
    (.beginPath ctx2)
    (.arc ctx2 x y size 0 (* 2 Math/PI))
    (.fill ctx2)))))

(defn draw-and-update-streak!
 "Given one streak star map and dt, draw its streak with either a white or blue glow,
  then update or respawn if off-screen. Returns the new star map."
 [{:keys [angle radius speed accel blue?] :as star} dt]
 (let [cx      (/ @width 2)
       cy      (/ @height 2)
       prev-r  radius
       prev-x  (+ cx (* (Math/cos angle) prev-r))
       prev-y  (+ cy (* (Math/sin angle) prev-r))

       ;; update speed & radius
       new-speed (+ speed (* accel dt))
       new-r     (+ radius (* new-speed dt))

       new-x (+ cx (* (Math/cos angle) new-r))
       new-y (+ cy (* (Math/sin angle) new-r))

       ctx2   @ctx

       ;; choose colors based on :blue?
       stroke-col   (if blue? "rgba(100,180,255,0.9)" "rgba(255,255,255,0.9)")
       shadow-col   (if blue? "rgba(100,180,255,0.7)" "rgba(255,255,255,0.8)")]

  (when ctx2
   ;; apply a small glow
   (set! (.-shadowBlur   ctx2) (random-range 1 3))
   (set! (.-shadowColor  ctx2) shadow-col)
   (set! (.-strokeStyle  ctx2) stroke-col)
   (set! (.-lineWidth    ctx2) (random-range 0.7 2.0))

   (.beginPath ctx2)
   (.moveTo ctx2 prev-x prev-y)
   (.lineTo ctx2 new-x new-y)
   (.stroke ctx2)

   ;; reset shadow so static stars (or next streak) aren't affected
   (set! (.-shadowBlur ctx2) 0))

  ;; If the streak moved off-screen, respawn as a fresh star (with new :blue? chance)
  (if (or (< new-x 0) (> new-x @width) (< new-y 0) (> new-y @height))
   (make-streak-star)
   ;; else, return updated star with new radius & speed (keeping same :blue? flag)
   {:angle  angle
    :radius new-r
    :speed  new-speed
    :accel  accel
    :blue?  blue?})))

(defn clear-canvas!
 "Clear the entire canvas to black each frame."
 []
 (let [ctx2 @ctx]
  (when ctx2
   (set! (.-globalCompositeOperation ctx2) "source-over")
   (set! (.-fillStyle ctx2) "#000")
   (.fillRect ctx2 0 0 @width @height))))

;; -------------------------------------------------------------------
;; 3) ANIMATION LOOP
;; -------------------------------------------------------------------

(defn animate
 "Main loop: clear, draw static stars, then draw & update all streak stars. Schedule next frame."
 [timestamp]
 (let [prev-time (.-_lastTime @ctx)
       dt        (if prev-time
                  (- timestamp prev-time)
                  16)]
  ;; store lastTime on the context object
  (set! (.-_lastTime @ctx) timestamp)

  ;; 1) Clear to black
  (clear-canvas!)

  ;; 2) Draw static background stars
  (draw-static-stars!)

  ;; 3) Update & draw every streak star
  (swap! streak-stars
         (fn [arr]
          (vec
           (for [star arr]
            (draw-and-update-streak! star dt)))))

  ;; 4) Next frame
  (.requestAnimationFrame js/window animate)))

;; -------------------------------------------------------------------
;; 4) RESIZE HANDLER + BOOTSTRAP
;; -------------------------------------------------------------------

(defn on-resize!
 "When the window resizes: update canvas dimensions and re-generate static stars for the new area."
 [_]
 ;; 1) Resize canvas
 (init-canvas-size!)
 ;; 2) Re-grab context
 (init-context!)
 ;; 3) Recreate static stars to fill new area
 (init-static-stars! 200))   ;; 200 static background stars

(defn ^:export start-hyperspace!
 "Entry point: set up canvas, static + streak stars, animation loop, and resize listener."
 []
 ;; 1) Initial canvas size & context
 (init-canvas-size!)
 (init-context!)
 ;; 2) Generate static background stars (200)
 (init-static-stars! 200)
 ;; 3) Generate fast-moving streak stars (800)
 (init-streak-stars! 800)
 ;; 4) Kick off the animation loop
 (.requestAnimationFrame js/window animate)
 ;; 5) Listen for resize so we stay full-screen
 (events/listen js/window EventType/RESIZE on-resize!))

;; Auto-start once the page loads
(.addEventListener js/window "load" start-hyperspace!)
