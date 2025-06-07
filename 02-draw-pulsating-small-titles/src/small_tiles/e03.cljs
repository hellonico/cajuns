(ns small-tiles.e03
 (:require [goog.dom :as gdom])
 (:import [goog.events EventType]))

;; ----------------------------------------------------------------------------
;; 1. Grab the <canvas> element and its 2D context. We'll set it to full‐screen.
;; ----------------------------------------------------------------------------

(def canvas (gdom/getElement "my-canvas"))
(def ctx    (.getContext canvas "2d"))

;; ----------------------------------------------------------------------------
;; 2. Create and load the Image. As soon as it finishes loading, we'll call init-squares!
;; ----------------------------------------------------------------------------

(def img (js/Image.))
(set! (.-src img) "https://static.posters.cz/image/750webp/144696.webp")
;; ──> Replace the above with your actual image path or URL.

;; ----------------------------------------------------------------------------
;; 3. State atoms:
;;
;;    • tile-size  : each tile is this many pixels on-screen (width & height).
;;    • squares    : a vector of maps, one per tile, storing
;;                   screen‐coordinates + source‐coordinates + random phase.
;;    • mode       : which pulsation style (0, 1, or 2).
;;    • mouse-pos  : [mx my], current mouse position (relative to canvas).
;; ----------------------------------------------------------------------------

(def tile-size
 24)
;; You can tweak tile-size (e.g. 16, 32, 8, etc.)

(defonce squares
         (atom []))
;; Each entry will be a map:
;; {
;;   :sx            ; screen X (top-left) of this tile
;;   :sy            ; screen Y (top-left) of this tile
;;   :center-x      ; screen X of tile’s center
;;   :center-y      ; screen Y of tile’s center
;;   :src-x         ; source-image X (top-left) corresponding to this tile
;;   :src-y         ; source-image Y (top-left)
;;   :src-w         ; source-image width for this tile
;;   :src-h         ; source-image height for this tile
;;   :phase         ; random phase ∈ [0, 2π) (used only by mode 0)
;; }

(defonce mode
         (atom 0))
;; 0 = random-phase
;; 1 = horizontal wave (from mouse.x)
;; 2 = radial circles (from mouse)

;; Initialize mouse at center of screen; will update on mousemove.
(defonce mouse-pos
         (atom [0 0]))

;; ----------------------------------------------------------------------------
;; 4. init-squares! – called when img “load” fires.
;;    • Resize the <canvas> to fill the browser window.
;;    • Also set CSS so that canvas truly is fixed, top-left, 100%×100%.
;;    • Compute scale factors for mapping “screen‐tile” → “source‐image‐tile.”
;;    • Compute how many columns/rows of tile-size fit into the screen.
;;    • For each (row, col), build one map entry for that tile:
;;        – screen‐coords  (sx, sy, center-x, center-y)
;;        – source‐coords  (src-x, src-y, src-w, src-h)
;;        – random phase   (for mode 0)
;;
;;    • Randomly pick mode ∈ {0,1,2}.
;;    • Initialize mouse-pos to the center of the canvas (just so mode 2 & 1 aren’t undefined).
;; ----------------------------------------------------------------------------

(defn init-squares! []
 ;; 1. Measure the loaded image’s natural size:
 (let [img-w     (.-width img)
       img-h     (.-height img)
       ;; 2. Grab the window’s innerWidth/innerHeight so we make the canvas full-screen:
       canvas-w  (.-innerWidth js/window)
       canvas-h  (.-innerHeight js/window)

       ;; 3. Compute scale factors to map from “screen pixels” to “image pixels”:
       ;;    If we stretch the image to exactly cover the canvas, then
       ;;      every screen‐pixel (x',y') corresponds to image‐pixel
       ;;      ( x' * (img-w / canvas-w),  y' * (img-h / canvas-h) ).
       scale-x   (/ img-w  canvas-w)
       scale-y   (/ img-h  canvas-h)

       ;; 4. How many full tile-size squares fit horizontally/vertically?
       cols      (js/Math.floor (/ canvas-w tile-size))
       rows      (js/Math.floor (/ canvas-h tile-size))

       ;; 5. The maximum distance (in screen coords) from any point to any point is the diagonal:
       ;;    We'll use this for normalizing distances in mode 2.
       max-dist  (js/Math.sqrt (+ (* canvas-w canvas-w)
                                  (* canvas-h canvas-h)))]

  ;; --- Resize the canvas drawing‐buffer to match the window size: ---
  (set! (.-width  canvas) canvas-w)
  (set! (.-height canvas) canvas-h)

  ;; --- Also force the CSS so that <canvas> truly covers the viewport: ---
  (set! (.-position (.-style canvas))   "fixed")
  (set! (.-top      (.-style canvas))   "0px")
  (set! (.-left     (.-style canvas))   "0px")
  (set! (.-width    (.-style canvas))   "100%")
  (set! (.-height   (.-style canvas))   "100%")
  (set! (.-margin   (.-style canvas))   "0px")
  (set! (.-padding  (.-style canvas))   "0px")
  (set! (.-display  (.-style canvas))   "block")
  (set! (.-userSelect (.-style canvas)) "none")

  ;; --- Pick a random mode ∈ {0,1,2}: ---
  (reset! mode
          (js/Math.floor (* 3 (js/Math.random))))
  ;; Log which mode was chosen (for debugging):
  (case @mode
   0 (js/console.log "Pulsation mode: 0 (random-phase)")
   1 (js/console.log "Pulsation mode: 1 (horizontal wave from mouse.x)")
   2 (js/console.log "Pulsation mode: 2 (radial circles from mouse)"))

  ;; --- Initialize mouse-pos to the screen’s center: ---
  (reset! mouse-pos [(/ canvas-w 2) (/ canvas-h 2)])

  ;; --- Build the vector of tile‐metadata: ---
  ;; For each row=0→rows-1, col=0→cols-1, compute:
  ;; screen top-left: (sx = col*tile-size, sy = row*tile-size)
  ;; screen center   : (center-x = sx + tile-size/2, center-y = sy + tile-size/2)
  ;; source top-left : (src-x = sx*scale-x, src-y = sy*scale-y)
  ;; source size     : (src-w = tile-size*scale-x, src-h = tile-size*scale-y)
  ;; random phase    : (rand ∈ [0,2π))
  (let [tile-maps
        (for [row (range rows)
              col (range cols)]
         (let [sx        (* col tile-size)
               sy        (* row tile-size)
               center-x  (+ sx (/ tile-size 2.0))
               center-y  (+ sy (/ tile-size 2.0))
               ;; Map that 24×24 screen‐tile back to the image’s pixel space:
               src-x     (* sx scale-x)
               src-y     (* sy scale-y)
               src-w     (* tile-size scale-x)
               src-h     (* tile-size scale-y)
               phase     (* 2 Math/PI (js/Math.random))]
          {:sx       sx
           :sy       sy
           :center-x center-x
           :center-y center-y
           :src-x    src-x
           :src-y    src-y
           :src-w    src-w
           :src-h    src-h
           :phase    phase}))]
   (reset! squares (vec tile-maps)))))

;; When the image finishes loading, run init-squares!:
(.addEventListener img "load" init-squares!)

;; ----------------------------------------------------------------------------
;; 5. Track the mouse position over the canvas. We update `mouse-pos` on each move.
;; ----------------------------------------------------------------------------

(.addEventListener canvas
                   "mousemove"
                   (fn [e]
                    (let [rect (.getBoundingClientRect canvas)
                          mx   (- (.-clientX e) (.-left rect))
                          my   (- (.-clientY e) (.-top  rect))]
                     (reset! mouse-pos [mx my]))))

;; ----------------------------------------------------------------------------
;; 6. The animation loop: draw-frame.
;;
;;    On each repaint:
;;     1. Clear the canvas.
;;     2. For each tile, compute a `scale` according to `@mode` and `@mouse-pos`.
;;     3. Draw from the source image’s rectangle → destination square.
;;     4. Request the next animation frame.
;; ----------------------------------------------------------------------------

(defn draw-frame [timestamp]
 (let [;; Convert ms → seconds
       t         (/ timestamp 1000.0)

       ;; Common sine parameters:
       base-scale 0.6
       amplitude  0.4
       two-pi     (* 2 Math/PI)

       ;; Current mode and mouse position:
       current-mode @mode
       [mx my]      @mouse-pos

       ;; Read the canvas’s width/height (should match window.innerWidth/innerHeight):
       canvas-w     (.-width canvas)
       canvas-h     (.-height canvas)

       ;; For mode 2 (radial), we’ll normalize by the diagonal length:
       max-dist     (js/Math.sqrt (+ (* canvas-w canvas-w)
                                     (* canvas-h canvas-h)))]

  ;; 1. Clear the entire screen:
  (.clearRect ctx 0 0 canvas-w canvas-h)

  ;; 2. Draw each tile with its computed scale:
  (doseq [{:keys [sx sy center-x center-y src-x src-y src-w src-h phase]}
          @squares]

   ;; Compute `scale` based on the chosen mode:
   (let [scale
         (case current-mode

          ;; -------- Mode 0: random-phase per tile (mouse has no effect on phase) --------
          0 (let [θ (+ (* two-pi t) phase)]
             ;; Oscillate between (base-scale − amplitude) and (base-scale + amplitude)
             (+ base-scale (* amplitude (js/Math.sin θ))))

          ;; -------- Mode 1: horizontal wave emanating from mouse.x --------
          ;; Compute dx = (tile-center-x − mx).  Then normalize by canvas width:
          ;;   normalized-x ∈ [−1, +1] if tile is left/right of mouse.
          ;; Wave argument = (2π·t) + (2π · normalized-x)
          ;; So at t=0, tiles exactly at mouse.x have sin(0) = 0. As time moves,
          ;; the wave travels left/right from the mouse pointer.
          1 (let [dx            (- center-x mx)
                  normalized-x  (/ dx canvas-w)
                  θ             (+ (* two-pi t)
                                   (* two-pi normalized-x))]
             (+ base-scale (* amplitude (js/Math.sin θ))))

          ;; -------- Mode 2: radial circles from the mouse pointer --------
          ;; Compute Euclidean distance from tile-center to [mx,my]:
          ;;   dist = sqrt((cx−mx)^2 + (cy−my)^2)
          ;; Normalize by max-dist:
          ;;   dist-norm ∈ [0, 1].  Then do sin(2π·(t − dist-norm)).
          ;; At t = dist-norm, sin(0) = 0 → that ring is at base-size. As t advances,
          ;; rings expand outward from the mouse.
          2 (let [dx        (- center-x mx)
                  dy        (- center-y my)
                  dist      (js/Math.sqrt (+ (* dx dx) (* dy dy)))
                  dist-norm (/ dist max-dist)
                  θ         (* two-pi (- t dist-norm))]
             (+ base-scale (* amplitude (js/Math.sin θ))))

          ;; -------- Default/fallback: simple uniform pulsation around mouse center --------
          (+ base-scale (* amplitude (js/Math.sin (* two-pi t)))) )]

    ;; Destination size of this tile on‐screen:
    (let [dw       (* tile-size scale)
          dh       dw
          ;; We want to keep it centered on (center-x, center-y):
          dx       (- center-x (/ dw 2.0))
          dy       (- center-y (/ dh 2.0))]

     ;; Finally, draw from the source‐image into that destination square:
     ;; (.drawImage image, srcX, srcY, srcW, srcH,  dstX,  dstY,  dstW,  dstH)
     (.drawImage ctx img
                 src-x src-y src-w src-h
                 dx    dy    dw    dh))))

  ;; 3. Request next frame:
  (js/requestAnimationFrame draw-frame)))


(defn ^:export init []
 ;; Kick off the animation loop (once), so it keeps re‐scheduling itself:
 (js/requestAnimationFrame draw-frame))
