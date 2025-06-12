(ns my-app.core
 (:require
  [quil.core :as q]
  [quil.middleware :as m]   ;; → make sure you have this for fun‐mode, etc.
  [reagent.core :as r]
  [reagent.dom :as rd]
  ["@tensorflow/tfjs" :as tf]))

(defn remap [val in-min in-max out-min out-max]
 (+ out-min (* (/ (- val in-min) (- in-max in-min))
               (- out-max out-min))))

;; === State Atoms ===
(defonce app-state (r/atom {:training?     false
                            :learning-rate 0.1
                            :epochs        50}))
(defonce loss-history (r/atom []))
(defonce predictions (r/atom []))
(defonce model (atom nil))
(defonce epoch-num (r/atom 0))

(def xs (tf/tensor1d #js [1 2 3 4]))
(def ys (tf/tensor1d #js [2 4 6 8]))
(def viz-x (tf/tensor1d #js [0 1 2 3 4 5]))

;; === TF Model Setup ===
(defn create-model [lr]
 (let [m (tf/sequential)]
  (.add m (tf/layers.dense #js {:units 1 :inputShape #js [1]}))
  (.compile m #js {:optimizer (tf/train.sgd lr)
                   :loss      "meanSquaredError"})
  m))

(defn update-predictions! [m]
 (-> (.predict m viz-x)
     (.array)
     (.then #(reset! predictions %))))

(defn train-model! []
 (reset! loss-history [])
 (reset! epoch-num 0)
 (let [{:keys [epochs]} @app-state
       m @model]
  (.fit m xs ys
        #js {:epochs    epochs
             :callbacks #js {:onEpochEnd
                             (fn [_ logs]
                              (when (:training? @app-state)
                               (swap! epoch-num inc)
                               (swap! loss-history conj (.-loss logs))
                               (update-predictions! m)) )}})))

;; === Quil Setup ===
(defn setup []
 (q/frame-rate 30)
 (q/color-mode :rgb 255)
 (q/background 245))

(defn draw-grid [w h x-steps y-steps]
 ;; Draw light grid lines every unit on a 0–5 x 0–10 coordinate system
 (q/stroke 220) ; light gray
 (q/stroke-weight 1)
 ;; vertical lines
 (doseq [i (range (inc x-steps))]
  (let [x-px (remap i 0 x-steps 0 w)]
   (q/line x-px 0 x-px h)))
 ;; horizontal lines
 (doseq [j (range (inc y-steps))]
  (let [y-px (remap j 0 y-steps h 0)]
   (q/line 0 y-px w y-px)))
 ;; Draw darker axes at x=0 and y=0
 (q/stroke 180) ; medium gray
 (q/stroke-weight 2)
 ;; y‐axis at x=0 (left)
 (q/line 0 0 0 h)
 ;; x‐axis at y=0 (bottom)
 (q/line 0 h w h))

(defn draw []
 (let [w (q/width)
       h (q/height)
       ;; True data points on [0..5]
       points-x  [0 1 2 3 4 5]
       true-y     (map #(* 2 %) points-x)
       pred-y     @predictions
       max-y      10
       ;; inset (loss‐curve) dimensions
       inset-w    150
       inset-h    100
       inset-pad  10
       losses     @loss-history
       max-loss   (apply max (cons 1 losses))]

  ;; 1) Clear background & draw main grid/axes
  (q/background 245)
  (draw-grid w h 5 max-y)

  ;; 2) Draw “true” data points as filled blue circles
  (q/fill 30 144 255)   ; DodgerBlue
  (q/no-stroke)
  (doseq [[x y] (map vector points-x true-y)]
   (let [px (remap x 0 5 0 w)
         py (remap y 0 max-y h 0)]
    (q/ellipse px py 8 8)))

  ;; 3) Draw current regression‐line (predicted) as a red line
  ;;    We have pred-y for x=0 and x=5; draw a line between those two
  (when (and (seq pred-y) (= (count pred-y) (count points-x)))
   (let [y0  (nth pred-y 0)
         y5  (nth pred-y 5)
         px0 (remap 0 0 5 0 w)
         py0 (remap y0 0 max-y h 0)
         px5 (remap 5 0 5 0 w)
         py5 (remap y5 0 max-y h 0)]
    (q/stroke 220 20 60)   ; Crimson
    (q/stroke-weight 3)
    (q/no-fill)
    (q/line px0 py0 px5 py5)))

  ;; 4) Draw predicted points as small transparent red circles on top of the line
  (when (seq pred-y)
   (q/fill 220 20 60 180) ; semi‐transparent Crimson
   (q/no-stroke)
   (doseq [[x y] (map vector points-x pred-y)]
    (let [px (remap x 0 5 0 w)
          py (remap y 0 max-y h 0)]
     (q/ellipse px py 6 6))))

  ;; 5) Draw inset loss‐curve in top‐left corner
  (let [ix   inset-pad
        iy   inset-pad
        iw   inset-w
        ih   inset-h]
   ;; background for inset
   (q/fill 255 255 255 200) ; white with slight transparency
   (q/no-stroke)
   (q/rect ix iy iw ih 6)

   ;; axes for inset
   (q/stroke 150)
   (q/stroke-weight 1)
   ;; x‐axis
   (q/line ix (+ iy ih) (+ ix iw) (+ iy ih))
   ;; y‐axis
   (q/line ix iy ix (+ iy ih))

   ;; loss‐curve polyline (if we have any points)
   (when (seq losses)
    (q/no-fill)
    (q/stroke 255 97 3)   ; a bright orange‐red
    (q/stroke-weight 2)
    (q/begin-shape)
    (doseq [[i v] (map-indexed vector losses)]
     (let [px (remap i 0 (max 1 (dec (count losses))) ix (+ ix iw))
           py (remap v 0 max-loss (+ iy ih) iy)]
      (q/vertex px py)))
    (q/end-shape))))

 ;; 6) Draw text overlays (epoch count & current loss) bottom right
 (q/fill 50)
 (q/text-size 14)
 (q/text (str "Epoch: " @epoch-num)
         (- w 100) (- h 20))
 (when-let [last-loss (last losses)]
  (q/text (str "Loss: " (.toFixed last-loss 4))
          (- w 100) (- h 40))))

;; === Reagent UI ===
(defn control-panel []
 (let [{:keys [training? learning-rate epochs]} @app-state]
  [:div
   [:button {:on-click (fn []
                        (if training?
                         (swap! app-state assoc :training? false)
                         (do
                          (reset! model (create-model learning-rate))
                          (swap! app-state assoc :training? true)
                          (train-model!))))}
    (if training? "Pause Training" "Start Training")]

   [:div
    "Learning Rate: "
    [:input {:type      "range"
             :min       0.01 :max 1 :step 0.01
             :value     learning-rate
             :on-change #(let [v (js/parseFloat (.. % -target -value))]
                          (swap! app-state assoc :learning-rate v))}]
    (str learning-rate)]

   [:div [:strong "Epoch: "] @epoch-num]
   [:div
    "Epochs: "
    [:input {:type      "range"
             :min       1 :max 100 :step 1
             :value     epochs
             :on-change #(swap! app-state assoc :epochs (js/parseInt (.. % -target -value)))}]
    (str epochs)]]))

(defn app-root []
 [:div
  [:h2 {:style {:font-family "Helvetica, Arial, sans-serif"
                :margin-bottom "8px"}} "Neural Net Visualizer"]
  [control-panel]
  [:div {:id "quil-canvas"
         :style {:border "1px solid #ccc"
                 :margin "10px 0"}}]])

(defn ^:export init []
 ;; 1) Render the Reagent DOM so <div id="quil-canvas"> actually exists
 (rd/render [app-root]
            (.getElementById js/document "app"))
 ;; 2) Now Quil can mount to #quil-canvas
 (q/defsketch train-viz
              :host "quil-canvas"
              :size [600 400]       ;; wider/taller for a bit more room
              :setup setup
              :draw draw
              :middleware [m/fun-mode]))
