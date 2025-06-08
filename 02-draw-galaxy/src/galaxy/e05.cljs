;; galaxy_simulation.cljs
;; Refactored elliptical galaxy using SceneObject abstraction

(ns galaxy.e05
 (:require [goog.dom :as gdom]
           [goog.events :as events]))

;; --- State Atoms ---
(defonce objects (atom []))
(defonce planets (atom []))
(defonce sun-angle (atom 0))
(defonce last-time (atom 0))

;; --- Settings ---
(defonce settings
         (atom {:num-stars      800
                :num-clouds     20
                :num-comets     5
                :orbit-spacing  60
                :sun-speed      0.00005
                :sun-flicker    {:base 15 :range 3}
                :planet-speed   0.000001
                :cloud-speed    {:x 0.01 :y 0.005}
                :comet-speed    0.2
                :base-orbits    [120 180 240 300 360 420]
                :planet-colors  ["rgba(136,204,255,0.4)" "rgba(255,204,136,0.4)" "rgba(170,204,136,0.4)"
                                 "rgba(204,136,255,0.4)" "rgba(204,187,170,0.4)" "rgba(170,187,204,0.4)"]}))

;; --- Utility ---
(defn rand-range [min max] (+ min (* (rand) (- max min))))

;; --- SceneObject Protocol ---
(defprotocol SceneObject
 (update-object [this dt w h])
 (draw-object [this ctx]))

;; --- Stars ---
(defrecord Star [angle radius speed size color w h]
 SceneObject
 (update-object [this dt _ _]
  (assoc this :angle (mod (+ angle (* speed dt)) (* 2 js/Math.PI))))
 (draw-object [this ctx]
  (let [cx (/ w 2) cy (/ h 2)
        x (+ cx (* radius (js/Math.cos angle)))
        y (+ cy (* radius (js/Math.sin angle)))
        max-r (/ (js/Math.sqrt (+ (* w w) (* h h))) 2)
        alpha (+ 0.3 (* 0.7 (/ radius max-r)))]
   (set! (.-globalAlpha ctx) alpha)
   (set! (.-fillStyle ctx) color)
   (.beginPath ctx)
   (.arc ctx x y size 0 (* 2 js/Math.PI))
   (.fill ctx))))

;; --- Clouds ---
(defrecord Cloud [x y radius opacity dx dy w h]
 SceneObject
 (update-object [this dt _ _]
  (assoc this
   :x (mod (+ x (* dx dt)) w)
   :y (mod (+ y (* dy dt)) h)))
 (draw-object [this ctx]
  (let [r0 (* 0.1 radius)
        grad (.createRadialGradient ctx x y r0 x y radius)]
   (.addColorStop grad 0 (str "rgba(180,180,255," opacity ")"))
   (.addColorStop grad 1 "transparent")
   (set! (.-globalAlpha ctx) 1)
   (set! (.-fillStyle ctx) grad)
   (.beginPath ctx)
   (.arc ctx x y radius 0 (* 2 js/Math.PI))
   (.fill ctx))))

;; --- Comets ---
(defrecord Comet [x y angle speed length w h]
 SceneObject
 (update-object [this dt w h]
  (let [nx (+ x (* speed dt (js/Math.cos angle)))
        ny (+ y (* speed dt (js/Math.sin angle)))]
   (if (or (> nx w) (< nx 0) (> ny h) (< ny 0))
    (assoc this
     :x (rand-range 0 w)
     :y (rand-range 0 h)
     :angle (rand-range 0 (* 2 js/Math.PI)))
    (assoc this :x nx :y ny))))
 (draw-object [this ctx]
  (set! (.-strokeStyle ctx) "#ffffff")
  (set! (.-globalAlpha ctx) 0.8)
  (.beginPath ctx)
  (.moveTo ctx x y)
  (.lineTo ctx (- x (* length (js/Math.cos angle)))
           (- y (* length (js/Math.sin angle))))
  (.stroke ctx)))

;; --- Sun ---
(defrecord Sun [w h angle]
 SceneObject
 (update-object [this dt _ _]
  (assoc this :angle (mod (+ angle (* (:sun-speed @settings) dt)) (* 2 js/Math.PI))))
 (draw-object [this ctx]
  (let [sx (/ w 2) sy (/ h 2)]
   (set! (.-fillStyle ctx) "#ff8800")
   (.beginPath ctx)
   (.arc ctx sx sy 20 0 (* 2 js/Math.PI))
   (.fill ctx))))

;; --- Initialization & Animation ---
(defn init []
 (let [canvas (.querySelector js/document "#canvas")
       ctx (and canvas (.getContext canvas "2d"))
       dpr (or (.-devicePixelRatio js/window) 1)
       w (* dpr (.-innerWidth js/window))
       h (* dpr (.-innerHeight js/window))]
  (set! (.-width canvas) w)
  (set! (.-height canvas) h)
  (set! (.-style.width canvas) (str (/ w dpr) "px"))
  (set! (.-style.height canvas) (str (/ h dpr) "px"))
  (.scale ctx dpr dpr)

  (reset! objects
          (concat
           (repeatedly (:num-stars @settings)
                       #(->Star (rand-range 0 (* 2 js/Math.PI))
                                (rand-range 0 (/ w 2))
                                (rand-range 0.000001 0.00001)
                                (rand-range 0.3 0.8)
                                (rand-nth ["#ffffff" "#ffff66" "#aaaaaa"]) w h))
           (repeatedly (:num-clouds @settings)
                       #(->Cloud (rand-range 0 w) (rand-range 0 h) (rand-range 250 600)
                                 (rand-range 0.1 0.25) (rand-range -0.01 0.01)
                                 (rand-range -0.005 0.005) w h))
           (repeatedly (:num-comets @settings)
                       #(->Comet (rand-range 0 w) (rand-range 0 h)
                                 (rand-range 0 (* 2 js/Math.PI)) (:comet-speed @settings) 15 w h))
           [(->Sun w h 0)]))

  (.requestAnimationFrame js/window
                          (fn animate [t]
                           (let [dt (if (zero? @last-time) 0 (- t @last-time))]
                            (reset! last-time t)
                            (.clearRect ctx 0 0 w h)
                            (swap! objects (fn [objs] (mapv #(update-object % dt w h) objs)))
                            (doseq [obj @objects] (draw-object obj ctx)))
                           (.requestAnimationFrame js/window animate)))))

;(init)
