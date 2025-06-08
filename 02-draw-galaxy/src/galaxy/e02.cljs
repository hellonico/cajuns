;; galaxy_simulation.cljs
;; Elliptical galaxy with animated sun, anti-clockwise planets, centered system, spaced orbits, and moving moons.

(ns galaxy.e02)

;; State
(defonce stars     (atom []))
(defonce planets   (atom []))
(defonce clouds    (atom []))
(defonce sun-angle (atom 0))
(defonce last-time (atom 0))

;; Configuration
(def center-offset-x -220)  ;; shift system left by this pixels
(def num-stars     800)
(def num-clouds    30)
(def star-colors   ["#ffffff" "#ffff66" "#aaaaaa"])
;; Increased spacing for orbits
(def base-orbits   [120 180 240 300 360 420])
(def planet-colors ["#88ccff" "#ffcc88" "#aacc88" "#cc88ff" "#ccbbaa" "#aabbcc"])
(def sun-speed     0.00002)

;; Utility: random number
(defn rand-range [min max]
 (+ min (* (rand) (- max min))))

;; --- Star functions ---
(defn create-star [w h]
 (let [max-r (/ (js/Math.sqrt (+ (* w w) (* h h))) 2)]
  {:angle  (rand-range 0 (* 2 js/Math.PI))
   :radius (rand-range 0 max-r)
   :speed  (* 0.000003 (rand-range 0 max-r))
   :size   (rand-range 0.3 0.8)
   :color  (rand-nth star-colors)}))

(defn update-star [star dt]
 (update star :angle #(mod (+ % (* (:speed star) dt)) (* 2 js/Math.PI))))

(defn draw-star [ctx {:keys [angle radius size color]} w h]
 (let [cx (/ w 2) cy (/ h 2)
       x  (+ cx (* radius (js/Math.cos angle)))
       y  (+ cy (* radius (js/Math.sin angle)))
       max-r (/ (js/Math.sqrt (+ (* w w) (* h h))) 2)
       alpha (+ 0.3 (* 0.7 (/ radius max-r)))]
  (set! (.-globalAlpha ctx) alpha)
  (set! (.-fillStyle ctx) color)
  (.beginPath ctx)
  (.arc ctx x y size 0 (* 2 js/Math.PI))
  (.fill ctx)))

;; --- Sun functions ---
(defn update-sun [dt]
 (swap! sun-angle #(+ % (* sun-speed dt))))

(defn draw-sun [ctx w h]
 (let [center-x (+ (/ w 2) center-offset-x)
       center-y (/ h 2)
       ;; orbital radius of sun
       orb-r   30
       sx      (+ center-x (* orb-r (js/Math.cos @sun-angle)))
       sy      (+ center-y (* orb-r (js/Math.sin @sun-angle)))
       ;; flicker range increased
       r0      (+ 15 (rand-range -3 3))
       grad    (.createRadialGradient ctx sx sy 0 sx sy r0)]
  (.addColorStop grad 0   "rgba(255,200,0,1)")
  (.addColorStop grad 0.5 "rgba(255,80,0,0.7)")
  (.addColorStop grad 1   "rgba(255,0,0,0)")
  (set! (.-globalCompositeOperation ctx) "lighter")
  (set! (.-fillStyle ctx) grad)
  (.beginPath ctx)
  (.arc ctx sx sy r0 0 (* 2 js/Math.PI))
  (.fill ctx)
  ;; inner core larger
  (set! (.-fillStyle ctx) "#ff8800")
  (.beginPath ctx)
  (.arc ctx sx sy 10 0 (* 2 js/Math.PI))
  (.fill ctx)
  [sx sy]))

;; --- Planet & Moon functions ---
(defn create-planets []
 (mapv (fn [idx r]
        {:orbit-rx r
         :orbit-ry (* r 0.6)
         :angle     (rand-range 0 (* 2 js/Math.PI))
         :speed     (* 0.000001 r)   ;; slower planet speed
         :size      (+ 10 idx)   ;; bigger planets
         :color     (planet-colors idx)
         :moons     (mapv (fn [midx]
                           {:orbit-radius (+ 10 midx 6)
                            :angle        (rand-range 0 (* 2 js/Math.PI))
                            :speed        (* 0.0001 (+ 10 midx))
                            :size         2
                            :color        "#dddddd"})
                          [1 2])})
       (range)
       base-orbits))

(defn update-planet [p dt]
 (-> p
     (update :angle #(mod (+ % (* -1 (:speed p) dt)) (* 2 js/Math.PI)))
     (update :moons (fn [ms]
                     (mapv (fn [m]
                            (assoc m :angle (mod (+ (:angle m) (* -1 (:speed m) dt)) (* 2 js/Math.PI))))
                           ms)))))

(defn draw-planet [ctx {:keys [orbit-rx orbit-ry angle size color moons]} [sx sy]]
 (let [c    (js/Math.sqrt (- (* orbit-rx orbit-rx) (* orbit-ry orbit-ry)))
       cx0  (+ sx c)
       cy0  sy
       x    (+ cx0 (* orbit-rx (js/Math.cos angle)))
       y    (+ cy0 (* orbit-ry (js/Math.sin angle)))]
  ;; orbit path
  (set! (.-globalAlpha ctx) 0.2)
  (set! (.-strokeStyle ctx) "#ffffff")
  (set! (.-lineWidth ctx) 0.5)
  (.beginPath ctx)
  (.ellipse ctx cx0 cy0 orbit-rx orbit-ry 0 0 (* 2 js/Math.PI))
  (.stroke ctx)
  ;; planet
  (set! (.-globalAlpha ctx) 1)
  (set! (.-fillStyle ctx) color)
  (.beginPath ctx)
  (.arc ctx x y size 0 (* 2 js/Math.PI)) (.fill ctx)
  ;; moons
  (doseq [{:keys [orbit-radius angle size color]} moons]
   (let [mx (+ x (* orbit-radius (js/Math.cos angle)))
         my (+ y (* orbit-radius (js/Math.sin angle)))]
    (set! (.-fillStyle ctx) color)
    (.beginPath ctx)
    (.arc ctx mx my size 0 (* 2 js/Math.PI)) (.fill ctx)))))

;; --- Cloud functions ---
(defn create-cloud [w h]
 {:x       (rand-range 0 w)
  :y       (rand-range 0 h)
  :radius  (rand-range 120 300) ;; larger clouds
  :opacity (rand-range 0.1 0.25)
  ;; increase cloud drift speed
  :dx      (rand-range -0.01 0.01)  ;; reduced cloud drift speed
  ;:dy      (rand-range -0.005 0.005)  ;; reduced cloud drift speed})
  :dy      (rand-range -0.002 0.002)})

(defn update-cloud [c dt w h]
 (let [nx (mod (+ (:x c) (* (:dx c) dt)) w)
       ny (mod (+ (:y c) (* (:dy c) dt)) h)]
  (assoc c :x nx :y ny)))

(defn draw-cloud [ctx {:keys [x y radius opacity]}]
 (let [r0   (* 0.1 radius)
       r1   radius
       grad (.createRadialGradient ctx x y r0 x y r1)]
  (.addColorStop grad 0 (str "rgba(180,180,255," opacity ")"))
  (.addColorStop grad 1 "transparent")
  (set! (.-fillStyle ctx) grad)
  (.beginPath ctx)
  (.arc ctx x y radius 0 (* 2 js/Math.PI)) (.fill ctx)))

;; --- Animation Loop ---
(defn animate [ctx w h timestamp]
 (let [dt (if (zero? @last-time) 0 (- timestamp @last-time))]
  (reset! last-time timestamp)
  (update-sun dt)
  (swap! stars   (fn [ss] (mapv #(update-star % dt) ss)))
  (swap! planets (fn [ps] (mapv #(update-planet % dt) ps)))
  (swap! clouds  (fn [cs] (mapv #(update-cloud % dt w h) cs)))
  (.clearRect ctx 0 0 w h)
  (doseq [c @clouds] (draw-cloud ctx c))
  (doseq [s @stars] (draw-star ctx s w h))
  (let [sun-pos (draw-sun ctx w h)]
   (doseq [p @planets] (draw-planet ctx p sun-pos)))
  (.requestAnimationFrame js/window (fn [t] (animate ctx w h t)))))

;; --- Initialization ---
(defn init []
 (let [canvas (.querySelector js/document "#canvas")
       ctx    (and canvas (.getContext canvas "2d"))
       w      (.-innerWidth js/window)
       h      (.-innerHeight js/window)]
  (if-not ctx
   (js/console.error "Canvas or context missing.")
   (do
    (set! (.-width canvas) w)
    (set! (.-height canvas) h)
    (set! (.-globalCompositeOperation ctx) "lighter")
    (reset! last-time 0)
    (reset! stars   (vec (repeatedly num-stars   #(create-star w h))))
    (reset! planets (create-planets))
    (reset! clouds  (vec (repeatedly num-clouds  #(create-cloud w h))))
    (animate ctx w h 0)))))

;; Auto-start
;(init)
