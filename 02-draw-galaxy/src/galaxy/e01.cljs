(ns galaxy.e01
 (:require
  [goog.dom :as gdom]))

(defonce ctx (atom nil))
(defonce width (atom 0))
(defonce height (atom 0))
(defonce stars (atom []))

(defn random-range
 "Return a random float between lo and hi."
 [lo hi]
 (+ lo (* (rand) (- hi lo))))

(defn init-canvas!
 "Grab the canvas element, set dimensions, get 2D context."
 []
 (let [canvas (gdom/getElement "galaxy-canvas")]
  (reset! width (.-innerWidth js/window))
  (reset! height (.-innerHeight js/window))
  (set! (.-width canvas) @width)
  (set! (.-height canvas) @height)
  (reset! ctx (.getContext canvas "2d"))))

(defn make-star
 "Create a single star with a starting angle along a spiral arm."
 []
 (let [;_ ; Number of spiral arms
         arms 7
       ;_ ; Choose a random arm index [0 .. arms-1]
               arm-idx (rand-int arms)
       ;_ ; Base angle for this arm
               base-angle (* arm-idx (/ (* 2 Math/PI) arms))
       ;_ ; Random spread along the arm
               arm-angle (+ base-angle (random-range -0.3 0.3))
       ;_ ; Distance from center
               dist (random-range 20 (min @width @height)  ; up to half screen
                                  )]
  {:angle       arm-angle
   :radius      dist
   :size        (random-range 0.5 1.8)
   :spin-speed  (* 0.0001 (rand))     ; very slow spin
   :drift-speed (random-range -0.01 0.01) ; slight radial drift
   :brightness  (random-range 0.5 1.0)}))

(defn init-stars!
 "Populate `stars` atom with N star-maps scattered in a spiral."
 [N]
 (reset! stars
         (vec (for [_ (range N)]
               (make-star)))))

(defn update-star
 "Given one star map, return an updated star map after dt milliseconds."
 [{:keys [angle radius spin-speed drift-speed] :as star} dt]
 (let [new-angle  (+ angle (* spin-speed dt))
       new-radius (+ radius (* drift-speed dt))
       ;; if a star drifts too far or too close, wrap it
       clamped-radius
       (cond
        (< new-radius 5)
        (random-range (* 0.4 @width) (* 0.8 @width))

        (> new-radius (max @width @height))
        (random-range 20 (* 0.3 (min @width @height)))

        :else
        new-radius)]
  (assoc star
   :angle  new-angle
   :radius clamped-radius)))


(defn draw-star!
 "Render one star onto the canvas context."
 [{:keys [angle radius size brightness]}]
 (let [x (+ (/ @width 2) (* (Math/cos angle) radius))
       y (+ (/ @height 2) (* (Math/sin angle) radius))
       ctx2 @ctx
       alpha (min 1.0 brightness)]
  (when (and ctx2 (<= 0 x @width) (<= 0 y @height))
   (set! (.-globalAlpha ctx2) alpha)
   ;; pale bluish-white star color
   (set! (.-fillStyle ctx2) (str "rgba(200,220,255," alpha ")"))
   ;; draw a small circle
   (.beginPath ctx2)
   (.arc ctx2 x y size 0 (* 2 Math/PI))
   (.fill ctx2))))

(defn clear-canvas!
 "Draw a very translucent rectangle to create a persistent glow trail."
 []
 (let [ctx2 @ctx]
  (set! (.-globalCompositeOperation ctx2) "destination-in")
  (set! (.-fillStyle ctx2) "rgba(0, 0, 0, 0.05)")
  (.fillRect ctx2 0 0 @width @height)
  (set! (.-globalCompositeOperation ctx2) "source-over")))

(defn animate
 "Main loop: update & draw stars, then request next frame."
 [timestamp]
 (let [dt (or (- timestamp (or (.-_lastTime @ctx) timestamp)) 16)]
  ;; store lastTime so next dt computes
  (set! (.-_lastTime @ctx) timestamp)
  (clear-canvas!)
  ;; update all stars and draw them
  (swap! stars (fn [arr]
                (vec (for [star arr]
                      (let [updated (update-star star dt)]
                       (draw-star! updated)
                       updated)))))
  (.requestAnimationFrame js/window animate)))

(defn ^:export init
 "Entry point: initialize everything and kick off the loop."
 []
 (init-canvas!)
 (init-stars! 2000)           ;; choose ~800 stars
 ;; Prepare the canvas background gradient once
 (let [ctx2 @ctx
       grad (.createRadialGradient ctx2
                                   (/ @width 2) (/ @height 2) 0
                                   (/ @width 2) (/ @height 2) (* @width @height 0.7))]
  (.addColorStop grad 0 "rgba(30,30,60,0.2)")
  (.addColorStop grad 0.7 "rgba(0,0,0,0.9)")
  (set! (.-fillStyle ctx2) grad)
  (.fillRect ctx2 0 0 @width @height))
 ;; Start animation
 (.requestAnimationFrame js/window animate))
