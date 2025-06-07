(ns small-tiles.e01
 (:require [goog.dom :as gdom]
           [cljs.core.async :refer [<!]]
           [clojure.string :as str])
 (:import [goog.events EventType]))

;; ----------------------------------------------------------------------------
;; 1. Grab a reference to the <canvas> (assumes you have <canvas id="my-canvas">).
;; ----------------------------------------------------------------------------

(def canvas (gdom/getElement "my-canvas"))
(def ctx (.getContext canvas "2d"))

;; ----------------------------------------------------------------------------
;; 2. Create a JavaScript Image object and point it at your URL.
;; ----------------------------------------------------------------------------

(def img (js/Image.))
(set! (.-src img) "https://static.posters.cz/image/750webp/144696.webp")
;; ──> Replace "path/to/your/image.jpg" with your actual image path or URL.

;; ----------------------------------------------------------------------------
;; 3. Parameters for our grid of “tiles.”
;; ----------------------------------------------------------------------------

(def tile-size 24)
;;    Each tile is tile-size × tile-size pixels (in the source image).

(defonce squares
         (atom []))
;;    We’ll store a vector of maps {:sx :sy :phase} for each tile.

;; ----------------------------------------------------------------------------
;; 4. When the image finishes loading:
;;    • Resize the canvas to match the image’s dimensions.
;;    • Build the “squares” vector with one entry per tile.
;; ----------------------------------------------------------------------------

(defn init-squares! []
 (let [w (.-width img)
       h (.-height img)]
  ;; Resize our canvas to match the image exactly:
  (set! (.-width canvas) w)
  (set! (.-height canvas) h)

  ;; Build a sequence of all (x,y) origins for a grid of tile-size squares:
  ;; E.g. x = 0, tile-size, 2*tile-size, … as long as x < w.
  ;; Similarly for y.
  (let [xs (range 0 w tile-size)
        ys (range 0 h tile-size)]

   ;; For each (x,y), store a map with:
   ;;  - :sx, :sy   => the top-left corner within the source image.
   ;;  - :phase     => a random offset so not all tiles pulsate in unison.
   (reset! squares
           (for [sy ys
                 sx xs]
            {:sx    sx
             :sy    sy
             :phase (* 2 Math/PI (rand))})))))

;; Wire up the “load” event:
(.addEventListener img
                   "load"
                   init-squares!)

;; ----------------------------------------------------------------------------
;; 5. Animation loop.
;;
;;    At each frame:
;;      • Clear the canvas.
;;      • For each tile:
;;          – Compute scale = base + amplitude * sin(2π·time + phase).
;;          – Compute the on‐screen destination size = tile-size × scale.
;;          – Offset the draw position so it stays centered on (sx,sy).
;;          – Use ctx.drawImage(img, sx, sy, tile-size, tile-size, dx, dy, dw, dh).
;; ----------------------------------------------------------------------------

(defn draw-frame [timestamp]
 ;; timestamp is in milliseconds; convert to seconds for sine‐wave:
 (let [t (/ timestamp 1000.0)
       base-scale 0.6
       amplitude 0.4
       two-pi (* 2 Math/PI)]

  ;; 1. Clear entire canvas:
  (.clearRect ctx 0 0 (.-width canvas) (.-height canvas))

  ;; 2. Iterate over each square and draw it with a pulsating scale:
  (doseq [{:keys [sx sy phase]} @squares]
   ;; Compute a smooth scale in [base-scale - amplitude, base-scale + amplitude]:
   (let [scale (+ base-scale
                  (* amplitude
                     (js/Math.sin (+ (* two-pi t) phase))))
         ;; Destination size (dw=dh) is tile-size × scale:
         dw (* tile-size scale)
         dh dw
         ;; We want to keep the center of the tile at (sx + tile-size/2, sy + tile-size/2),
         ;; so dx = centerX - dw/2, dy = centerY - dh/2:
         center-x (+ sx (* 0.5 tile-size))
         center-y (+ sy (* 0.5 tile-size))
         dx (- center-x (* 0.5 dw))
         dy (- center-y (* 0.5 dh))]

    ;; Draw: source rect = (sx, sy, tile-size, tile-size)
    ;;       dest   rect = (dx, dy, dw, dh)
    (.drawImage ctx img
                sx sy                                       ; source x, y
                tile-size tile-size                         ; source width, height
                dx dy                                       ; destination x, y
                dw dh)))                                    ; destination width, height

  ;; 3. Request next frame:
  (js/requestAnimationFrame draw-frame)))

(defn ^:export init []
 ;; Kick off the animation loop (once), so it keeps re‐scheduling itself:
 (js/requestAnimationFrame draw-frame))
