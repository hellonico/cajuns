(ns small-tiles.e02
 (:require [goog.dom :as gdom]
           [cljs.core.async :refer [<!]]
           [clojure.string :as str])
 (:import [goog.events EventType]))

;; ----------------------------------------------------------------------------
;; 1. Grab the <canvas> and 2D context as before.
;; ----------------------------------------------------------------------------

(def canvas (gdom/getElement "my-canvas"))
(def ctx    (.getContext canvas "2d"))

;; ----------------------------------------------------------------------------
;; 2. Create an Image and point it to your source.
;; ----------------------------------------------------------------------------

(def img (js/Image.))
(set! (.-src img) "https://static.posters.cz/image/750webp/144696.webp")
;; Replace the above path with the actual URL or local path to your image.

;; ----------------------------------------------------------------------------
;; 3. Grid parameters and state atoms:
;;    - tile-size: the width/height (in px) of each square slice.
;;    - squares: a vector of maps, one per tile.
;;    - mode: which pulsation style to use (0, 1, or 2).
;; ----------------------------------------------------------------------------

(def tile-size 24)

(defonce squares
         (atom []))
;; Each entry will be {:sx :sy :phase :phase-wave :dist-norm}

(defonce mode
         (atom 0))
;; 0 = random phases, 1 = horizontal wave, 2 = radial circles

;; ----------------------------------------------------------------------------
;; 4. init-squares! – called when the image loads.
;;    • Resize the canvas to match the image.
;;    • Compute a grid of tile‐metadata maps. Depending on the mode:
;;       - For mode 0: we give each tile a random `:phase`.
;;       - For mode 1: we can precompute a “wave phase” based on tile’s x‐index.
;;       - For mode 2: we compute each tile’s normalized distance from the image center.
;;    • Randomly pick one mode on load.
;; ----------------------------------------------------------------------------

(defn init-squares! []
 (let [w      (.-width img)
       h      (.-height img)
       cols   (Math/floor (/ w tile-size))
       rows   (Math/floor (/ h tile-size))
       cx     (/ w 2.0)           ;; image-center x
       cy     (/ h 2.0)           ;; image-center y
       max-d  (Math/sqrt (+ (Math/pow cx 2)
                            (Math/pow cy 2)))]

  ;; 1. Resize canvas to match image dimensions:
  (set! (.-width  canvas) w)
  (set! (.-height canvas) h)

  ;; 2. Pick a random mode in {0,1,2}:
  (reset! mode (js/Math.floor (* 3 (js/Math.random))))
  ;; (0 ≤ mode ≤ 2)

  ;; 3. Build our vector of tile‐metadata:
  ;;    We'll iterate over all grid cells (row 0 → rows-1, col 0 → cols-1).
  ;;    For each tile:
  ;;      • :sx, :sy      → pixel coords (top-left) in source image
  ;;      • :phase        → a random [0,2π) (used by mode 0)
  ;;      • :phase-wave   → use (col / cols) × 2π as a “static” wave-phase
  ;;      • :dist-norm    → (distance from center)/max-d  (0 ≤ d ≤ 1)
  (reset! squares
          (for [row (range rows)
                col (range cols)]
           (let [sx         (* col tile-size)
                 sy         (* row tile-size)
                 center-x   (+ sx (/ tile-size 2.0))
                 center-y   (+ sy (/ tile-size 2.0))
                 dx         (- center-x cx)
                 dy         (- center-y cy)
                 dist       (Math/sqrt (+ (* dx dx) (* dy dy)))
                 dist-norm  (/ dist max-d)
                 phase      (* 2 Math/PI (js/Math.random))
                 phase-wave (* 2 Math/PI (/ col (dec cols)))]
            {:sx         sx
             :sy         sy
             :phase      phase
             :phase-wave phase-wave
             :dist-norm  dist-norm})))))

;; Wire up the image “load” event to init-squares!:
(.addEventListener img "load" init-squares!)

;; ----------------------------------------------------------------------------
;; 5. draw-frame – the animation loop.
;;    Each frame, for each tile we compute a scale factor according to `@mode`:
;;
;;    • mode 0 (“random phases”): exactly as before—
;;        scale = base + amp · sin(2π·t + :phase)
;;
;;    • mode 1 (“horizontal wave”):
;;        scale = base + amp · sin(2π·t + :phase-wave)
;;        Here :phase-wave depends only on the column index, so the wave
;;        travels horizontally as t changes.
;;
;;    • mode 2 (“radial circles”):
;;        scale = base + amp · sin(2π·(t – :dist-norm))
;;        All tiles at the same distance‐norm pulse together; as time advances,
;;        the rings appear to expand/contract.
;;
;;    You can tweak base-scale and amplitude to taste.
;; ----------------------------------------------------------------------------

(defn draw-frame [timestamp]
 (let [t          (/ timestamp 1000.0)   ;; convert ms → seconds
       base-scale 0.6
       amplitude  0.4
       two-pi     (* 2 Math/PI)
       current-mode @mode]

  ;; 1. Clear entire canvas:
  (.clearRect ctx 0 0 (.-width canvas) (.-height canvas))

  ;; 2. Draw each tile with a mode-specific scale:
  (doseq [{:keys [sx sy phase phase-wave dist-norm]} @squares]
   ;; Compute `scale` based on mode:
   (let [scale
         (case current-mode

          ;; -------- mode 0: random-phase per tile --------
          0 (let [θ (+ (* two-pi t) phase)]
             (+ base-scale (* amplitude (js/Math.sin θ))))

          ;; -------- mode 1: horizontal wave --------
          ;; sin(2π t + phase-wave) where phase-wave ∈ [0,2π)
          ;; is a static offset based on column index → wave travels in x.
          1 (let [θ (+ (* two-pi t) phase-wave)]
             (+ base-scale (* amplitude (js/Math.sin θ))))

          ;; -------- mode 2: radial circles from center--------
          ;; sin(2π · (t − dist-norm)) makes rings expand/contract
          2 (let [θ (* two-pi (- t dist-norm))]
             (+ base-scale (* amplitude (js/Math.sin θ))))

          ;; default fallback (shouldn’t happen):
          (+ base-scale (* amplitude (js/Math.sin (* two-pi t)))))]

    ;; Destination size of this tile:
    (let [dw       (* tile-size scale)
          dh       dw
          center-x (+ sx (/ tile-size 2.0))
          center-y (+ sy (/ tile-size 2.0))
          dx       (- center-x (/ dw 2.0))
          dy       (- center-y (/ dh 2.0))]
     ;; Draw that slice of the source image:
     (.drawImage ctx img
                 sx sy           ;; source x, y
                 tile-size tile-size ;; source width, height
                 dx dy           ;; dest x, y
                 dw dh))))       ;; dest width, height

  ;; 3. Request next animation frame:
  (js/requestAnimationFrame draw-frame)))
;
;;; Kick off the loop once:
;(js/requestAnimationFrame draw-frame)

;; ----------------------------------------------------------------------------
;; 6. (Optional) Log which mode was chosen, for debugging:
;; ----------------------------------------------------------------------------

(.addEventListener img
                   "load"
                   (fn []
                    (case @mode
                     0 (js/console.log "Pulsation mode: 0 (random-phase)")
                     1 (js/console.log "Pulsation mode: 1 (horizontal wave)")
                     2 (js/console.log "Pulsation mode: 2 (radial circles)"))))

(defn ^:export init []
 ;; Kick off the animation loop (once), so it keeps re‐scheduling itself:
 (js/requestAnimationFrame draw-frame))
