(ns pong.core
 (:require
  [reagent.core :as r]
  [reagent.dom :as rd]))

(defonce app-state (r/atom {:paddle-x 0
                            :ball-x 0
                            :ball-y 0
                            :ball-vx 1
                            :ball-vy 1
                            :running? false
                            :score 0
                            :hits 0
                            :canvas-width 800 ;; Default, updated dynamically
                            :canvas-height 600}))

(def paddle-width 150)
(def paddle-height 10)

(defn update-canvas-size []
 (let [canvas (.getElementById js/document "game-canvas")
       width (.-width canvas)
       height (.-height canvas)]
  (swap! app-state assoc
         :canvas-width width
         :canvas-height height
         :paddle-x (/ width 2)
         :ball-x (/ width 2)
         :ball-y (/ height 2))))

(defn draw-canvas []
 (let [canvas (.getElementById js/document "game-canvas")
       ctx (.getContext canvas "2d")
       {:keys [paddle-x ball-x ball-y running? score canvas-width canvas-height]} @app-state]
  (.clearRect ctx 0 0 canvas-width canvas-height) ;; Clear canvas

  ;; Draw paddle
  (set! (.-fillStyle ctx) "white")
  (.fillRect ctx paddle-x (- canvas-height 40) paddle-width paddle-height)

  ;; Draw ball if running
  (when running?
   (.fillRect ctx ball-x ball-y 10 10))

  ;; Draw score
  (set! (.-fillStyle ctx) "white")
  (set! (.-font ctx) "20px Arial")
  (.fillText ctx (str "Score: " score) 20 30)

  ;; "Click to Start" message
  (when-not running?
   (set! (.-font ctx) "30px Arial")
   (.fillText ctx "Click to Start" (- (/ canvas-width 2) 80) (/ canvas-height 2)))))

(defn reset-game []
 (update-canvas-size)
 (swap! app-state assoc
        :ball-vx 1
        :ball-vy 1
        :running? false
        :score 0
        :hits 0)
 (draw-canvas))

(defn start-new-game []
 (update-canvas-size)
 (swap! app-state assoc
        :running? true
        :score 0
        :hits 0
        :ball-x (/ (:canvas-width @app-state) 2)
        :ball-y (/ (:canvas-height @app-state) 2)
        :ball-vx 4
        :ball-vy 4)
 (draw-canvas))

(defn update-game []
 (when (:running? @app-state)
  (swap! app-state
         (fn [{:keys [ball-x ball-y ball-vx ball-vy paddle-x score hits canvas-width canvas-height] :as state}]
          (let [new-x (+ ball-x ball-vx)
                new-y (+ ball-y ball-vy)
                hit-wall-x (or (< new-x 0) (> new-x (- canvas-width 10)))
                hit-wall-y (< new-y 0)
                hit-paddle (and (> new-y (- canvas-height 50))
                                (< new-x (+ paddle-x paddle-width))
                                (> new-x paddle-x))
                miss-paddle (> new-y canvas-height)
                new-hits (if hit-paddle (inc hits) hits)
                speed-increase (if (and hit-paddle (= (mod new-hits 5) 0))
                                (* 1.2 (js/Math.sign ball-vy)) 1)
                new-vy (if hit-paddle (* -1 ball-vy speed-increase) ball-vy)]
           (if miss-paddle
            (reset-game)
            (-> state
                (assoc :ball-x new-x)
                (assoc :ball-y new-y)
                (update :ball-vx (fn [vx] (if hit-wall-x (- vx) vx)))
                (update :ball-vy (fn [vy] (if hit-wall-y (- vy) new-vy)))
                (update :score (fn [s] (if hit-paddle (inc s) s)))
                (assoc :hits new-hits))))))
  (draw-canvas)))

(defn move-paddle [dx]
 (swap! app-state update :paddle-x #(-> % (+ dx) (max 0) (min (- (:canvas-width @app-state) paddle-width))))
 (draw-canvas))

(defn mouse-move-handler [e]
 (let [canvas (.getElementById js/document "game-canvas")
       rect (.getBoundingClientRect canvas)
       mouse-x (- (.-clientX e) (.-left rect))]
  (swap! app-state assoc :paddle-x (-> mouse-x (- (/ paddle-width 2)) (max 0) (min (- (:canvas-width @app-state) paddle-width))))))

(defn mouse-click-handler [_]
 (when-not (:running? @app-state)
  (start-new-game)))

(defn key-down-handler [e]
 (case (.-key e)
  "ArrowLeft" (move-paddle -20)
  "ArrowRight" (move-paddle 20)
  nil))

(defn start-game []
 (js/setInterval update-game 16)
 (.addEventListener js/document "keydown" key-down-handler)
 (.addEventListener js/document "mousemove" mouse-move-handler)
 (.addEventListener js/document "click" mouse-click-handler))

(defn init []
 (update-canvas-size)
 (reset-game)
 (start-game))

(init)
