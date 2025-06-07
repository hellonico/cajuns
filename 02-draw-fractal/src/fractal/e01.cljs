(ns fractal.e01
 (:require [goog.dom :as gdom]))

;; ------------------------------------------------------------
;; 1. Canvas size and max‐iteration
;; ------------------------------------------------------------
(def width 800)
(def height 600)
(def max-iter 500)   ; raise for more detail (slower), lower for speed

;; ------------------------------------------------------------
;; 2. Compute “continuous” escape‐time (μ) for each c = (cr + i·ci)
;;
;;    Returns:
;;      - If c ∈ Mandelbrot set (no escape in max-iter), returns max-iter.
;;      - Otherwise, returns a double μ = iter + 1 − log₂(log₂ |z|).
;; ------------------------------------------------------------
(defn mandelbrot-continuous
 [cr ci]
 (loop [zr    0.0
        zi    0.0
        iter  0]
  (let [zr2 (+ (* zr zr) 0.0)
        zi2 (+ (* zi zi) 0.0)
        r2  (+ zr2 zi2)]
   (if (and (< iter max-iter) (< r2 4.0))
    ;; zₙ₊₁ = zₙ² + c
    (let [new-zr (+ (- zr2 zi2) cr)
          new-zi (+ (* 2 zr zi) ci)]
     (recur new-zr new-zi (inc iter)))
    ;; EXIT: either escaped or reached max-iter
    (if (>= iter max-iter)
     ;; considered “inside” set
     max-iter
     ;; compute μ = n + 1 − log₂(log₂ |z|)
     (let [absz (js/Math.sqrt r2)
           ; log₂ x = (log x)/(log 2)
           mu    (+ iter
                    (- 1.0
                       (/ (js/Math.log (js/Math.log absz))
                          (js/Math.log 2.0))))]
      mu))))))

;; ------------------------------------------------------------
;; 3. Map continuous μ → HSL color string
;;
;;    - μ in [0, max-iter) maps to hue in [0°, 360°).
;;    - Inside set (μ == max-iter) → black.
;;    - Adjust saturation/lightness to taste.
;; ------------------------------------------------------------
(defn μ->hsl
 [μ]
 (if (>= μ max-iter)
  ;; interior → black
  "hsl(0, 0%, 0%)"
  ;; otherwise map to a hue
  (let [t    (/ μ max-iter)         ; normalized [0,1)
        hue  (* 360.0 t)            ; [0,360)
        sat  80                      ; 80% saturation
        lit  50                      ; 50% lightness
        h    (js/Math.floor hue)]
   (str "hsl(" h "," sat "%," lit "%)"))))

;; ------------------------------------------------------------
;; 4. Draw one scan‐line at y, then schedule y+1 via requestAnimationFrame
;; ------------------------------------------------------------
(defn draw-row
 [ctx y]
 (when (< y height)
  (doseq [x (range width)]
   ;; Map pixel→complex in rectangle [−2.5, +1.0] × [−1.0, +1.0]
   (let [cr  (+ -2.5 (* x (/ 3.5 width)))
         ci  (+ -1.0 (* y (/ 2.0 height)))
         μ   (mandelbrot-continuous cr ci)
         col (μ->hsl μ)]
    (set! (.-fillStyle ctx) col)
    (.fillRect ctx x y 1 1)))
  (js/requestAnimationFrame
   (fn []
    (draw-row ctx (inc y))))))

;; ------------------------------------------------------------
;; 5. Initialization: size the <canvas> and start at row 0
;; ------------------------------------------------------------
(defn init
 []
 (let [canvas (gdom/getElement "fractal-canvas")
       ctx    (.getContext canvas "2d")]
  (.setAttribute canvas "width"  (str width))
  (.setAttribute canvas "height" (str height))
  (draw-row ctx 0)))

;; Attach init to window load
;(.addEventListener js/window "load" init)
