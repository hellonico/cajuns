;; galaxy_simulation.cljs
;; Refactored elliptical galaxy using SceneObject abstraction

(ns galaxy.e07
 (:require [goog.dom :as gdom]
           [goog.events :as events]
           [goog.style :as gstyle]))

;; --- State Atoms ---
(defonce objects (atom []))
(defonce last-time (atom 0))
(defonce stats-visible? (atom false))
(defonce last-dt (atom 0))

;; --- Settings ---
(defonce settings
         (atom {:num-stars     1200
                :num-clouds    20
                :num-comets    5
                :num-planets   6
                :orbit-spacing 160
                :sun-speed     0.00005
                :sun-flicker   {:base 15 :range 3}
                :planet-speed  0.0000001
                :cloud-speed   {:x 0.01 :y 0.005}
                :comet-speed   0.2
                :audio         true
                :base-orbits   [120 180 240 300 360 420]
                :planet-colors ["rgba(136,204,255,0.4)" "rgba(255,204,136,0.4)" "rgba(170,204,136,0.4)"
                                "rgba(204,136,255,0.4)" "rgba(204,187,170,0.4)" "rgba(170,187,204,0.4)"]}))

(defonce audio-context
         (when (.-AudioContext js/window)
          (new js/AudioContext)))
(defonce background-music (atom nil))

(defn init-audio []
 (when (and audio-context (:audio @settings))
  (let [audio (new js/Audio (str "/audio/" (rand-nth ["inner-peace.mp3" "breath-of-life.mp3" "morning-in-the-forest.mp3" "perfect-beauty.mp3" "zen-garden.mp3"])))]
   (set! (.-loop audio) true)
   (.play audio)
   (reset! background-music audio))))

;; --- Utility ---
(defn rand-range [min max] (+ min (* (rand) (- max min))))

(defn draw-stats [ctx w _h]
 (when @stats-visible?
  (.save ctx) ;; Save current canvas state

  (let [padding 12
        line-height 24
        stars   (count (filter #(instance? Star %) @objects))
        planets (count (filter #(instance? Planet %) @objects))
        comets  (count (filter #(instance? Comet %) @objects))
        total   (count @objects)
        fps     (if (pos? @last-dt) (int (/ 1000 @last-dt)) 0)
        audio-on? (boolean @background-music)
        dpr     (.-devicePixelRatio js/window)
        lines   [(str "OBJECTS: " total)
                 (str "STARS:   " stars)
                 (str "PLANETS: " planets)
                 (str "COMETS:  " comets)
                 (str "FPS:     " fps)
                 (str "AUDIO:   " (if audio-on? "ON" "OFF"))
                 (str "SCALE:   " (js/Math.round (* dpr 100)) "%")]
        box-width 260
        box-height (+ 20 (* line-height (count lines)))
        right-x (- w box-width padding)
        top-y padding]

   ;; Reset transform
   (.setTransform ctx 1 0 0 1 0 0)

   ;; Background
   (set! (.-globalAlpha ctx) 0.9)
   (set! (.-fillStyle ctx) "rgba(0,0,0,0.8)")
   (.fillRect ctx right-x top-y box-width box-height)

   ;; Border
   (set! (.-strokeStyle ctx) "#00ff66")
   (set! (.-lineWidth ctx) 2)
   (.strokeRect ctx right-x top-y box-width box-height)

   ;; Scanlines
   (set! (.-fillStyle ctx) "rgba(0,255,100,0.05)")
   (doseq [i (range top-y (+ top-y box-height) 4)]
    (.fillRect ctx right-x i box-width 1))

   ;; Text style
   (set! (.-font ctx) "bold 18px monospace")
   (set! (.-fillStyle ctx) "#00ff66")
   (set! (.-shadowColor ctx) "#00ff66")
   (set! (.-shadowBlur ctx) 8)
   (set! (.-globalAlpha ctx) 1)

   ;; Text lines
   (doseq [[i line] (map-indexed vector lines)]
    (.fillText ctx (.toUpperCase line)
               (+ right-x 14)
               (+ top-y (* line-height (+ i 1))))))

  (.restore ctx))) ;; Restore original canvas state




;; --- SceneObject Protocol ---
(defprotocol SceneObject
 (update-object [this dt w h])
 (draw-object [this ctx]))

;; --- Stars ---
(defrecord Star [angle radius speed size color w h]
 SceneObject
 (update-object [this dt _ _]
  (update this :angle #(mod (+ % (* speed dt)) (* 2 js/Math.PI))))
 (draw-object [this ctx]
  (let [cx (/ w 2) cy (/ h 2)
        x (+ cx (* radius (js/Math.cos angle)))
        y (+ cy (* radius (js/Math.sin angle)))]
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
   (.addColorStop grad 0 (str "rgba(180,180,255," (* opacity 0.6) ")"))
   (.addColorStop grad 1 "transparent")
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
  (let [sx (/ w 2) sy (/ h 2)
        t (/ (.now js/Date) 1000)
        base 25 range 5
        wave (+ base (* 3 (js/Math.sin (* t 2))))
        flicker (+ wave (* 2 (js/Math.sin (* t 6))))
        grad (.createRadialGradient ctx sx sy 0 sx sy flicker)]
   (.addColorStop grad 0 "rgba(255,200,0,1)")
   (.addColorStop grad 0.5 "rgba(255,80,0,0.7)")
   (.addColorStop grad 1 "rgba(255,0,0,0)")
   (set! (.-globalCompositeOperation ctx) "lighter")
   (set! (.-fillStyle ctx) grad)
   (.beginPath ctx)
   (.arc ctx sx sy flicker 0 (* 2 js/Math.PI))
   (.fill ctx)
   (set! (.-fillStyle ctx) "#ff8800")
   (.beginPath ctx)
   (.arc ctx sx sy (/ base 1.5) 0 (* 2 js/Math.PI))
   (.fill ctx))))


(defrecord Planet
 [orbit-radius angle speed size color w h moons offset-x offset-y opacity ring? ring-count ring-speed ring-rotation tilt]
 SceneObject
 (update-object [this dt _ _]
  (let [new-angle (mod (+ angle (* speed dt)) (* 2 js/Math.PI))
        updated-moons
        (mapv (fn [m]
               (update m :angle #(mod (+ % (* (:speed m) dt)) (* 2 js/Math.PI))))
              moons)]
   (assoc this :angle new-angle :moons updated-moons)))

 (draw-object [this ctx]
  (let [cx (/ w 2) cy (/ h 2)
        rx orbit-radius
        ry (* orbit-radius 0.6)
        ox (+ cx offset-x)
        oy (+ cy offset-y)
        x (+ ox (* rx (js/Math.cos angle)))
        y (+ oy (* ry (js/Math.sin angle)))]

   ;; Orbit path
   (set! (.-strokeStyle ctx) "#ffffff")
   (set! (.-globalAlpha ctx) 0.1)
   (set! (.-lineWidth ctx) 0.5)
   (.beginPath ctx)
   (.ellipse ctx ox oy rx ry 0 0 (* 2 js/Math.PI))
   (.stroke ctx)

   ;; Planet body
   (set! (.-fillStyle ctx) color)
   (set! (.-globalAlpha ctx) opacity)
   (.beginPath ctx)
   (.arc ctx x y size 0 (* 2 js/Math.PI))
   (.fill ctx)

   ;; Optional ring
   (when ring?
    (assoc this :ring-rotation (mod (+ ring-rotation ring-speed) (* 2 js/Math.PI)))
    (dotimes [i ring-count]
     (let [rx (+ (* size 1.8) (* i 1.5)) ; wider per ring
           ry (* rx 0.5)]
      (.save ctx)
      (.translate ctx x y)
      (.rotate ctx tilt)             ;; 🌌 TILT preserved here
      (.rotate ctx ring-rotation)   ;; 🌀 Ring's own rotation
      (set! (.-strokeStyle ctx) "rgba(255,255,255,0.4)")
      (set! (.-lineWidth ctx) 0.6)
      (.beginPath ctx)
      (.ellipse ctx 0 0 rx ry 0 0 (* 2 js/Math.PI))
      (.stroke ctx)
      (.restore ctx))))




   ;; Moons
   (doseq [{:keys [orbit-radius angle size color]} moons]
    (let [mx (+ x (* orbit-radius (js/Math.cos angle)))
          my (+ y (* orbit-radius (js/Math.sin angle)))]
     (set! (.-fillStyle ctx) color)
     (set! (.-globalAlpha ctx) 0.6)
     (.beginPath ctx)
     (.arc ctx mx my size 0 (* 2 js/Math.PI))
     (.fill ctx))))))





;; --- Overlay Menu ---
(declare init)
;; --- Overlay Menu ---


;; --- Overlay Menu ---
(defonce overlay-visible? (atom false))

(defn create-overlay []
 (let [overlay (gdom/createElement "div")]
  (set! (.-id overlay) "settings-overlay")
  (gstyle/setStyle overlay #js {:position   "fixed" :top "50%" :left "50%"
                                :transform  "translate(-50%, -50%)"
                                :background "rgba(0,0,0,0.8)" :color "#fff"
                                :padding    "20px" :borderRadius "10px"
                                :fontFamily "Arial, sans-serif" :zIndex 9999 :display "none"
                                :boxShadow  "0 0 20px rgba(255,255,255,0.2)" :textAlign "center"})
  (doseq [[k v] @settings]
   (let [row (gdom/createElement "div")
         label (gdom/createElement "label")
         input (gdom/createElement "input")]
    (set! (.-type input) "number")
    (set! (.-value input) v)
    (set! (.-style.width input) "80px")
    (set! (.-style.marginLeft input) "10px")
    (set! (.-style.padding input) "4px")
    (set! (.-style.borderRadius input) "4px")
    (set! (.-style.border input) "none")
    (set! (.-style.background input) "rgba(255,255,255,0.1)")
    (set! (.-style.color input) "#fff")
    (set! (.-style.textAlign input) "right")
    (gdom/setTextContent label (str (name k) ": "))
    (events/listen input "change"
                   (fn [_]
                    (swap! settings assoc k (js/parseFloat (.-value input)))
                    (init)))
    (gdom/append row label input)
    (gstyle/setStyle row #js {:marginBottom "10px" :display "flex" :justifyContent "space-between"})
    (gdom/appendChild overlay row)))
  (gdom/appendChild (.-body js/document) overlay)))


(defn toggle-overlay []
 (let [overlay (.querySelector js/document "#settings-overlay")]
  (if @overlay-visible?
   (do (set! (.-display (.-style overlay)) "none")
       (reset! overlay-visible? false))
   (do (set! (.-display (.-style overlay)) "block")
       (reset! overlay-visible? true)))))

(defn animate [ctx w h]
 (.requestAnimationFrame
  js/window
  (fn [t]
   (let [dt (if (zero? @last-time) 0 (- t @last-time))]

    ;; Clear and reset transform BEFORE drawing
    (.setTransform ctx 1 0 0 1 0 0)
    (.clearRect ctx 0 0 w h)

    ;; Update and draw scene
    (swap! objects #(mapv (fn [o] (update-object o dt w h)) %))
    (doseq [o @objects] (draw-object o ctx))

    ;; Reset transform again before HUD
    (.setTransform ctx 1 0 0 1 0 0)
    (set! (.-globalAlpha ctx) 1)
    (set! (.-globalCompositeOperation ctx) "source-over")

    ;; Update time
    (reset! last-dt dt)
    (reset! last-time t)
    (draw-stats ctx w h)


    ;; Loop again
    (animate ctx w h)))))


;; --- Initialization & Animation ---
(defn init []
 (init-audio)
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
                                (rand-range 0.000001 0.0001)
                                (rand-range 0.3 0.8)
                                (rand-nth ["#ffffff" "#ffff66" "#aaaaaa"]) w h))
           (repeatedly (:num-clouds @settings)
                       #(->Cloud (rand-range 0 w) (rand-range 0 h) (rand-range 250 600)
                                 (rand-range 0.1 0.25) (rand-range -0.01 0.01)
                                 (rand-range -0.005 0.005) w h))

           (map-indexed
            (fn [i _]
             (let [spacing (:orbit-spacing @settings)
                   base-radius (+ 220 (* i spacing))
                   size (+ 6 (mod i 4))
                   opacity (+ 0.45 (rand 0.25))
                   moon-count (rand-int 3) ; 0–2 moons
                   moons (mapv (fn [j]
                                {:orbit-radius (+ 10 (* j 6))
                                 :angle (rand-range 0 (* 2 js/Math.PI))
                                 :speed (+ 0.0003 (* 0.0002 j))
                                 :size 2
                                 :color "#dddddd"})
                               (range 1 (inc moon-count)))
                   ring? (< (rand) 0.5)
                   ring-color (rand-nth ["#66ffff" "#ff66ff" "#ffff66" "#ffffff"])]
              (->Planet base-radius
                        (rand-range 0 (* 2 js/Math.PI))
                        (* (:planet-speed @settings) base-radius)
                        size
                        (nth (:planet-colors @settings) (mod i 6))
                        w h
                        moons
                        (rand-range -50 50)
                        (rand-range -30 30)
                        opacity
                        (zero? (rand-int 2))        ; ring?
                        (rand-range (/ js/Math.PI 12) (/ js/Math.PI 4)) ; tilt
                        ;ring-color
                        )))
            (range (:num-planets @settings)))




           (repeatedly (:num-comets @settings)
                       #(->Comet (rand-range 0 w)
                                 (rand-range 0 h)
                                 (rand-range 0 (* 2 js/Math.PI))
                                 (:comet-speed @settings) 15
                                 w h))
           [(->Sun w h 0)]))

  (when-not (.querySelector js/document "#settings-overlay")
   (create-overlay))

  (animate ctx w h)))

(events/listen
 js/document
 "keydown"
 (fn [e]
  (when (= (.-key e) "m")
   (toggle-overlay))))

(events/listen
 js/document
 "keydown"
 (fn [e]
  (when (= (.-key e) "i")
   (swap! stats-visible? not))))