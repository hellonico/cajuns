;; src/galaxy/e02.cljs
(ns galaxy.e04
 (:require [goog.dom :as gdom]
           [goog.events :as events]))

;; --- State Atoms ---
(defonce stars (atom []))
(defonce planets (atom []))
(defonce clouds (atom []))
(defonce comets (atom []))
(defonce flare-timers (atom []))
(defonce sun-angle (atom 0))
(defonce camera (atom {:x 0 :y 0 :scale 1}))
(defonce tooltip-elm (atom nil))
(defonce last-time (atom 0))
(defonce trails (atom {}))

;; --- Settings ---
(defonce settings
         (atom {:num-stars     3600
                :num-clouds    30
                :num-comets    10
                :orbit-spacing 80
                :sun-orbit     30
                :sun-flicker   {:base 15 :range 3}
                :sun-speed     0.00005
                :planet-speed  0.000001
                :cloud-speed   {:x 0.01 :y 0.005}
                :comet-speed   0.005
                :show-rings    true
                :trail-length  120
                :day-night     true
                :audio         true
                :base-orbits   [120 180 240 300 360 420]
                :planet-colors ["#88ccff" "#ffcc88" "#aacc88" "#cc88ff" "#ccbbaa" "#aabbcc"]}))


;; --- Audio Setup ---
(defonce audio-context
         (when (.-AudioContext js/window)
          (new js/AudioContext)))
(defonce background-music (atom nil))

(defn init-audio []
 (when (and audio-context (:audio @settings))
  (let [audio (new js/Audio "/audio/inner-peace.mp3")]
   (set! (.-loop audio) true)
   (.play audio)
   (reset! background-music audio))))

;; --- Utility ---
(defn rand-range [min max] (+ min (* (rand) (- max min))))
(defn lerp [a b t] (+ (* a (- 1 t)) (* b t)))

;; --- Stars ---
(defn create-star [w h]
 (let [max-r (/ (js/Math.sqrt (+ (* w w) (* h h))) 2)]
  {:angle  (rand-range 0 (* 2 js/Math.PI))
   :radius (rand-range 0 max-r)
   :speed  (* (:planet-speed @settings) (rand-range 0 max-r))
   :size   (rand-range 0.3 0.8)
   :color  (rand-nth ["#ffffff" "#ffff66" "#aaaaaa"])}))

(defn update-star [s dt]
 (update s :angle #(mod (+ % (* (:speed s) dt)) (* 2 js/Math.PI))))

(defn draw-star [ctx {:keys [angle radius size color]} w h]
 (let [cx (/ w 2)
       cy (/ h 2)
       x (+ cx (* radius (js/Math.cos angle)))
       y (+ cy (* radius (js/Math.sin angle)))
       max-r (/ (js/Math.sqrt (+ (* w w) (* h h))) 2)
       alpha (+ 0.3 (* 0.7 (/ radius max-r)))]
  (set! (.-globalAlpha ctx) alpha)
  (set! (.-fillStyle ctx) color)
  (.beginPath ctx)
  (.arc ctx x y size 0 (* 2 js/Math.PI))
  (.fill ctx)))


;; --- Clouds (Parallax) ---
(defn create-cloud [w h]
 {:x       (rand-range 0 w)
  :y       (rand-range 0 h)
  :radius  (rand-range 250 600) ;; significantly larger clouds
  :opacity (rand-range 0.1 0.25)
  :dx      (rand-range -0.01 0.01)
  :dy      (rand-range -0.005 0.005)})


(defn update-cloud [c dt w h]
 (let [nx (mod (+ (:x c) (* (:dx c) dt)) w)
       ny (mod (+ (:y c) (* (:dy c) dt)) h)]
  (assoc c :x nx :y ny)))

(defn draw-cloud [ctx {:keys [x y radius opacity]}]
 (let [r0 (* 0.1 radius)
       r1 radius
       grad (.createRadialGradient ctx x y r0 x y r1)]
  (.addColorStop grad 0 (str "rgba(180,180,255," opacity ")"))
  (.addColorStop grad 1 "transparent")
  (set! (.-globalAlpha ctx) 1)
  (set! (.-fillStyle ctx) grad)
  (.beginPath ctx)
  (.arc ctx x y radius 0 (* 2 js/Math.PI))
  (.fill ctx)))

;; --- Comets / Shooting Stars ---
(defn spawn-comet [w h]
 {:x (rand-range 0 w)
  :y (rand-range 0 h)
  :angle (rand-range 0 (* 2 js/Math.PI))
  :speed 0.2
  :length 15})

(defn update-comet [comet w h dt]
 (let [{:keys [x y angle speed]} comet
       nx (+ x (* speed dt (js/Math.cos angle)))
       ny (+ y (* speed dt (js/Math.sin angle)))]
  (if (or (> nx w) (< nx 0) (> ny h) (< ny 0))
   (spawn-comet w h)
   (assoc comet :x nx :y ny))))

(defn draw-comet [ctx {:keys [x y angle length]}]
 (set! (.-strokeStyle ctx) "#ffffff")
 (set! (.-globalAlpha ctx) 0.8)
 (.beginPath ctx)
 (.moveTo ctx x y)
 (.lineTo ctx (- x (* length (js/Math.cos angle)))
          (- y (* length (js/Math.sin angle))))
 (.stroke ctx))


;; --- Solar Flares ---
(defn maybe-flare []
 (when (< (rand) 0.002)
  (swap! flare-timers conj 0)))

(defn draw-flares [ctx dt]
 (let [new-timers
       (keep (fn [t]
              (let [nt (+ t dt)]
               (when (< nt 1)
                (let [alpha (- 1 nt)
                      w (* 200 nt)]
                 (set! (.-globalAlpha ctx) alpha)
                 (set! (.-strokeStyle ctx) "orange")
                 (.beginPath ctx)
                 (.arc ctx (/ (.-width ctx) 2) (/ (.-height ctx) 2) w 0 (* 2 js/Math.PI))
                 (.stroke ctx))
                nt)))
             @flare-timers)]
  (reset! flare-timers new-timers)))

;; --- Sun ---
;(defonce settings
;         (atom {:sun-speed 0.00005})) ;; new setting, small value

(defn update-sun [dt]
 (swap! sun-angle #(+ % (* (:sun-speed @settings) dt))))

(defn draw-sun [ctx w h]
 (maybe-flare)
 (let [cx (/ w 2)
       cy (/ h 2)
       orb (:sun-orbit @settings)
       sx (+ cx (* orb (js/Math.cos @sun-angle)))
       sy (+ cy (* orb (js/Math.sin @sun-angle)))
       {:keys [base range]} (:sun-flicker @settings)
       r0 (+ base (rand-range (- range) range))
       grad (.createRadialGradient ctx sx sy 0 sx sy r0)]
  (.addColorStop grad 0 "rgba(255,200,0,1)")
  (.addColorStop grad 0.5 "rgba(255,80,0,0.7)")
  (.addColorStop grad 1 "rgba(255,0,0,0)")
  (set! (.-globalCompositeOperation ctx) "lighter")
  (set! (.-fillStyle ctx) grad)
  (.beginPath ctx)
  (.arc ctx sx sy r0 0 (* 2 js/Math.PI)) (.fill ctx)
  ;; inner core
  (set! (.-fillStyle ctx) "#ff8800")
  (.beginPath ctx)
  (.arc ctx sx sy (/ base 1.5) 0 (* 2 js/Math.PI)) (.fill ctx)
  [sx sy]))

;; --- Planets, Rings, Trails, Day/Night Shading ---
(defn create-planets []
 (mapv (fn [i base-r]
        (let [rspacing (:orbit-spacing @settings)]
         {:orbit-rx (+ base-r (* i rspacing))
          :orbit-ry (* (+ base-r (* i rspacing)) 0.6)
          :angle    (rand-range 0 (* 2 js/Math.PI))
          :speed    (* (:planet-speed @settings) (+ base-r (* i rspacing)))
          :size     (+ 10 i)
          :color    (nth (:planet-colors @settings) i)
          :moons    (mapv (fn [mi]
                           {:orbit-radius (+ 10 mi 6)
                            :angle        (rand-range 0 (* 2 js/Math.PI))
                            :speed        (* 0.0001 (+ 10 mi))
                            :size         2
                            :color        "#dddddd"})
                          [1 2])}))
       (range)
       (:base-orbits @settings)))

(defn update-planet [p dt]
 (-> p
     (update :angle #(mod (+ % (* (:speed p) dt)) (* 2 js/Math.PI)))
     (update :moons
             (fn [ms]
              (mapv (fn [m]
                     (assoc m :angle
                              (mod (+ (:angle m) (* (:speed m) dt))
                                   (* 2 js/Math.PI))))
                    ms)))))

(defn draw-ring [ctx cx cy rx ry]
 (when (:show-rings @settings)
  (set! (.-globalAlpha ctx) 0.2)
  (set! (.-strokeStyle ctx) "#ffffff")
  (set! (.-lineWidth ctx) 0.5)
  (.beginPath ctx)
  (.ellipse ctx cx cy rx ry 0 0 (* 2 js/Math.PI))
  (.stroke ctx)))

(defn draw-planet [ctx p [sx sy] trail-map]
 (let [{:keys [orbit-rx orbit-ry angle size color moons]} p
       c (js/Math.sqrt (- (* orbit-rx orbit-rx) (* orbit-ry orbit-ry)))
       cx0 (+ sx c)
       cy0 sy
       x (+ cx0 (* orbit-rx (js/Math.cos angle)))
       y (+ cy0 (* orbit-ry (js/Math.sin angle)))]
  ;; trail
  (when-let [pts (get @trails p)]
   (set! (.-strokeStyle ctx) color)
   (set! (.-lineWidth ctx) 1)
   (.beginPath ctx)
   (doseq [[tx ty] pts] (.lineTo ctx tx ty))
   (.stroke ctx))
  ;; ring
  (draw-ring ctx cx0 cy0 orbit-rx orbit-ry)
  ;; day/night shading
  (let [light-dir (js/Math.atan2 (- y sy) (- x sx))
        grad (.createRadialGradient ctx x y 0 x y size)]
   (.addColorStop grad 0 color)
   (.addColorStop grad 0.5 (if (:day-night @settings)
                            (str color "80")
                            color))
   (.addColorStop grad 1 "black")
   (set! (.-fillStyle ctx) grad)
   (.beginPath ctx)
   (.arc ctx x y size 0 (* 2 js/Math.PI))
   (.fill ctx))
  ;; moons
  (doseq [m moons]
   (let [mx (+ x (* (:orbit-radius m) (js/Math.cos (:angle m))))
         my (+ y (* (:orbit-radius m) (js/Math.sin (:angle m))))]
    (set! (.-fillStyle ctx) (:color m))
    (.beginPath ctx)
    (.arc ctx mx my (:size m) 0 (* 2 js/Math.PI))
    (.fill ctx)))))

;; --- Tooltips ---
(defn init-tooltip []
 (let [el (gdom/createDom "div" #js {:id "tooltip"})]
  (set! (.-style.position el) "absolute")
  (set! (.-style.padding el) "4px")
  (set! (.-style.background el) "rgba(0,0,0,0.7)")
  (set! (.-style.color el) "#fff")
  (set! (.-style.display el) "none")
  (reset! tooltip-elm el)
  (gdom/appendChild (.-body js/document) el)))

(defn show-tooltip [x y text]
 (when-let [el @tooltip-elm]
  (set! (.-innerText el) text)
  (set! (.-style.left el) (str x "px"))
  (set! (.-style.top el) (str y "px"))
  (set! (.-style.display el) "block")))

(defn hide-tooltip []
 (when-let [el @tooltip-elm]
  (set! (.-style.display el) "none")))

;; --- Zoom & Pan ---
(def dragging (atom false))
(def last-pos (atom nil))
(events/listen js/document "wheel"
               (fn [e]
                (swap! camera update :scale #(max 0.2 (min 5 (+ % (* -0.001 (.-deltaY e))))))))
(events/listen js/document "mousedown" (fn [e] (reset! dragging true) (reset! last-pos [(.-clientX e) (.-clientY e)])))
(events/listen js/document "mouseup" (fn [_] (reset! dragging false)))
(events/listen js/document "mousemove" (fn [e]
                                        (when @dragging
                                         (let [[lx ly] @last-pos
                                               cx (.-clientX e) cy (.-clientY e)]
                                          (swap! camera update :x #(+ % (- cx lx)))
                                          (swap! camera update :y #(+ % (- cy ly)))
                                          (reset! last-pos [cx cy])))))

(defn animate [ctx w h timestamp]
 (let [dt (if (zero? @last-time) 0 (- timestamp @last-time))]
  (reset! last-time timestamp)

  ;; occasionally spawn comets
  (when (< (rand) (/ (:num-comets @settings) 10000))
   (spawn-comet w h))

  ;; update entities
  (update-sun dt)
  (swap! stars (fn [ss] (mapv #(update-star % dt) ss)))
  (swap! planets (fn [ps] (mapv #(update-planet % dt) ps)))
  (swap! clouds (fn [cs] (mapv #(update-cloud % dt w h) cs)))
  (swap! comets #(mapv (fn [c] (update-comet c w h dt)) %))

  ;; clear entire canvas before transforming
  (set! (.-globalAlpha ctx) 1)
  (.setTransform ctx 1 0 0 1 0 0)
  (.clearRect ctx 0 0 w h)

  ;; apply camera transformations (zoom and pan)
  (.translate ctx (:x @camera) (:y @camera))
  (.scale ctx (:scale @camera) (:scale @camera))

  ;; draw clouds and stars
  (doseq [c @clouds] (draw-cloud ctx c))
  (doseq [s @stars] (draw-star ctx s w h))

  ;; draw sun only ONCE and cache position
  (let [sun-pos (draw-sun ctx w h)]

   ;; update trails using consistent planet positions
   (doseq [p @planets]
    (let [{:keys [orbit-rx orbit-ry angle]} p
          c (js/Math.sqrt (- (* orbit-rx orbit-rx) (* orbit-ry orbit-ry)))
          cx0 (+ (first sun-pos) c)
          cy0 (second sun-pos)
          x (+ cx0 (* orbit-rx (js/Math.cos angle)))
          y (+ cy0 (* orbit-ry (js/Math.sin angle)))]
     (swap! trails update p
            (fnil conj [])
            [x y])
     (when (> (count (@trails p)) (:trail-length @settings))
      (swap! trails update p subvec 1))))

   ;; now draw planets with stable sun-pos
   (doseq [p @planets]
    (draw-planet ctx p sun-pos @trails)))

  ;; draw comets and flares last
  (draw-flares ctx dt)
  (doseq [comet @comets]
   (draw-comet ctx comet))

  ;; restore transform after rendering
  (.setTransform ctx 1 0 0 1 0 0)

  ;; loop
  (.requestAnimationFrame js/window #(animate ctx w h %))))



(defn init []
 (init-audio)
 (let [canvas (.querySelector js/document "#canvas")
       ctx (and canvas (.getContext canvas "2d"))
       dpr (or (.-devicePixelRatio js/window) 1)
       w (* dpr (.-innerWidth js/window))
       h (* dpr (.-innerHeight js/window))]
  ;; Set canvas dimensions for HiDPI screens
  (set! (.-width canvas) w)
  (set! (.-height canvas) h)
  (set! (.-style.width canvas) (str (/ w dpr) "px"))
  (set! (.-style.height canvas) (str (/ h dpr) "px"))
  ;; Enable smooth rendering
  (set! (.-imageSmoothingEnabled ctx) true)
  (set! (.-imageSmoothingQuality ctx) "high")
  ;; Scale context
  (.scale ctx dpr dpr)

  ;; Populate entities
  (reset! stars (vec (repeatedly (:num-stars @settings) (fn [] (create-star w h)))))
  (reset! planets (create-planets))
  (reset! clouds (vec (repeatedly (:num-clouds @settings) (fn [] (create-cloud w h)))))
  (reset! comets (vec (repeatedly 3 #(spawn-comet w h))))
  (reset! last-time 0)

  ;; Begin animation loop
  (.requestAnimationFrame js/window #(animate ctx w h %))))

