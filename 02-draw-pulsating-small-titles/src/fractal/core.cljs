(ns fractal.core
 (:require [goog.dom :as gdom]))

;; ------------------------------------------------------------
;; 1. Final iteration cap (as before)
;; ------------------------------------------------------------
(def final-max-iter 500)

;; ------------------------------------------------------------
;; 2. Continuous escape‐time (same as before)
;; ------------------------------------------------------------
(defn mandelbrot-continuous
 [cr ci iter-limit]
 (loop [zr    0.0
        zi    0.0
        iter  0]
  (let [zr2 (+ (* zr zr) 0.0)
        zi2 (+ (* zi zi) 0.0)
        r2  (+ zr2 zi2)]
   (if (and (< iter iter-limit) (< r2 4.0))
    (let [new-zr (+ (- zr2 zi2) cr)
          new-zi (+ (* 2 zr zi) ci)]
     (recur new-zr new-zi (inc iter)))
    (if (>= iter iter-limit)
     iter-limit
     (let [absz (js/Math.sqrt r2)
           mu   (+ iter
                   (- 1.0
                      (/ (js/Math.log (js/Math.log absz))
                         (js/Math.log 2.0))))]
      mu))))))

;; ------------------------------------------------------------
;; 3. HSL → RGB conversion (we’ll need raw [r,g,b] values)
;; ------------------------------------------------------------
(defn hsl->rgb
 "Convert h∈[0,360), s∈[0,1], l∈[0,1] into [R G B] each ∈[0,255]."
 [h s l]
 (let [hf (/ h 60.0)
       c  (* (- 1.0 (js/Math.abs (- (* 2.0 l) 1.0))) s)
       x  (* c (- 1.0 (js/Math.abs (- (mod hf 2) 1.0))))
       [r1 g1 b1]
       (cond
        (< hf 1) [c   x  0.0]
        (< hf 2) [x   c  0.0]
        (< hf 3) [0.0 c  x  ]
        (< hf 4) [0.0 x  c  ]
        (< hf 5) [x   0.0 c]
        :else    [c   0.0 x])
       m (- l (/ c 2.0))
       r (+ r1 m)
       g (+ g1 m)
       b (+ b1 m)]
  [(js/Math.floor (* 255 r))
   (js/Math.floor (* 255 g))
   (js/Math.floor (* 255 b))]))

;; ------------------------------------------------------------
;; 4. Build & blit a single ImageData per frame
;; ------------------------------------------------------------
(defn draw-full-image
 "Given a 2D context `ctx` and an `iter-limit`, write _all_ pixels into
  a fresh ImageData buffer, then do one `putImageData`."
 [ctx iter-limit]
 (let [canvas  (.-canvas ctx)
       width   (.-width canvas)
       height  (.-height canvas)
       ;; create a blank ImageData (RGBA) for the full canvas
       image-data (.createImageData ctx width height)
       data       (.-data image-data)]
  ;; Loop over every (x,y) and compute the color bytes:
  (doseq [y (range height)
          x (range width)]
   (let [cr (+ -2.5 (* x (/ 3.5 width)))
         ci (+ -1.0 (* y (/ 2.0 height)))
         μ  (mandelbrot-continuous cr ci iter-limit)
         idx (* 4 (+ x (* y width)))]     ;; byte index = 4*(y*width + x)
    (if (>= μ iter-limit)
     ;; never escaped → black:
     (do
      (aset data idx     0)
      (aset data (+ idx 1) 0)
      (aset data (+ idx 2) 0)
      (aset data (+ idx 3) 255))
     ;; otherwise map μ→HSL→RGB bytes:
     (let [t   (/ μ iter-limit)          ; in [0,1)
           h   (* 360.0 t)               ; hue
           ;; s=0.8, l=0.5 (tweakable)
           [r g b] (hsl->rgb h 0.8 0.5)]
      (aset data idx     r)
      (aset data (+ idx 1) g)
      (aset data (+ idx 2) b)
      (aset data (+ idx 3) 255)))))
  ;; Blit all at once:
  (.putImageData ctx image-data 0 0)))

;; ------------------------------------------------------------
;; 5. Animation loop (only changed to call draw-full-image)
;; ------------------------------------------------------------
(defn animate
 [ctx]
 (let [current-iter (atom 1)]
  (letfn [(step []
           (let [n @current-iter
                 canvas (.-canvas ctx)]
            ;; (Optional) clear is not needed, since we'll overwrite all pixels
            ;; (.clearRect ctx 0 0 (.-width canvas) (.-height canvas))
            ;; Draw entire ImageData at iter-limit = n
            (draw-full-image ctx n)
            (when (< n final-max-iter)
             ;; e.g. bump by 1 or bump by 2 to halve the frames:
             (swap! current-iter inc)
             (js/requestAnimationFrame step))))]
   (js/requestAnimationFrame step))))

;; ------------------------------------------------------------
;; 6. Initialization: full‐screen canvas + launch
;; ------------------------------------------------------------
(defn init
 []
 (let [canvas (gdom/getElement "fractal-canvas")
       ctx    (.getContext canvas "2d")
       w      (.-innerWidth js/window)
       h      (.-innerHeight js/window)]
  ;; 6.1: Set buffer = full viewport
  (.setAttribute canvas "width"  (str w))
  (.setAttribute canvas "height" (str h))
  ;; 6.2: CSS to force full‐screen (no scrollbars/margins)
  (set! (.. canvas -style -position) "fixed")
  (set! (.. canvas -style -top)      "0")
  (set! (.. canvas -style -left)     "0")
  (set! (.. js/document -body -style -margin)  "0")
  (set! (.. js/document -body -style -overflow) "hidden")
  ;; 6.3: Start the faster ImageData‐based animation
  (animate ctx)))

;; Hook into window load
(.addEventListener js/window "load" init)
