(ns fractal.e02
 (:require [goog.dom :as gdom]))

;; ------------------------------------------------------------
;; 1. Final iteration cap (same as before)
;; ------------------------------------------------------------
(def final-max-iter 500)

;; ------------------------------------------------------------
;; 2. Continuous escape‐time up to a given `iter-limit`
;; ------------------------------------------------------------
(defn mandelbrot-continuous
 "Run at most `iter-limit` iterations on c = (cr + i·ci).
  - If it never escapes by `iter-limit`, return `iter-limit`.
  - Otherwise, return the smooth μ = n + 1 − log₂(log₂|z|) at escape."
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
;; 3. Map continuous μ → HSL (inside = black)
;; ------------------------------------------------------------
(defn μ->hsl
 [μ iter-limit]
 (if (>= μ iter-limit)
  ;; not escaped by `iter-limit` → black
  "hsl(0,0%,0%)"
  (let [t    (/ μ iter-limit)         ; normalized [0,1)
        hue  (* 360.0 t)               ; hue in [0,360)
        sat  80                        ; 80% saturation
        lit  50                        ; 50% lightness
        h    (js/Math.floor hue)]
   (str "hsl(" h "," sat "%," lit "%)"))))

 ;; ------------------------------------------------------------
 ;; 4. Draw every pixel using current `iter-limit`, but read width/height from the canvas itself
 ;; ------------------------------------------------------------
 (defn draw-full
  "Given a 2D context `ctx` and an iteration cutoff `iter-limit`, redraw all pixels
   from (0,0) to (canvas.width, canvas.height)."
  [ctx iter-limit]
  (let [canvas  (.-canvas ctx)
        width   (.-width canvas)
        height  (.-height canvas)]
   (doseq [y (range height)
           x (range width)]
    ;; Map pixel → complex c in [−2.5, +1.0] × [−1.0, +1.0]
    (let [cr  (+ -2.5 (* x (/ 3.5 width)))
          ci  (+ -1.0 (* y (/ 2.0 height)))
          μ   (mandelbrot-continuous cr ci iter-limit)
          col (μ->hsl μ iter-limit)]
     (set! (.-fillStyle ctx) col)
     (.fillRect ctx x y 1 1)))))

 ;; ------------------------------------------------------------
 ;; 5. Animation loop: increase `current-iter` until `final-max-iter`
 ;; ------------------------------------------------------------
 (defn animate
  "Keeps `current-iter` in an atom, starting at 1. Each frame:
     1. Clears the canvas (not strictly needed if we redraw every pixel).
     2. Calls `draw-full` with the current iteration limit.
     3. Increments `current-iter` and requests next frame (if ≤ final-max-iter)."
  [ctx]
  (let [current-iter (atom 1)]
   (letfn [(step []
            (let [n @current-iter
                  canvas (.-canvas ctx)]
             ;; (Optional) clear, though `draw-full` will overwrite every pixel
             (.clearRect ctx 0 0 (.-width canvas) (.-height canvas))
             ;; Draw full canvas at iteration = n
             (draw-full ctx n)
             ;; If we haven’t reached the final cap, bump & loop
             (when (< n final-max-iter)
              (swap! current-iter inc)
               ;(swap! current-iter #(+ % 2))
              (js/requestAnimationFrame step))))]
    ;; Kick off the loop
    (js/requestAnimationFrame step))))

 ;; ------------------------------------------------------------
 ;; 6. Initialization: size canvas to window, apply CSS, and begin
 ;; ------------------------------------------------------------
 (defn init
  []
  (let [canvas (gdom/getElement "fractal-canvas")
        ctx    (.getContext canvas "2d")
        w      (.-innerWidth js/window)
        h      (.-innerHeight js/window)]
   (.addEventListener js/window "resize"
                      (fn []
                       (let [w (.-innerWidth js/window)
                             h (.-innerHeight js/window)]
                        (.setAttribute canvas "width"  (str w))
                        (.setAttribute canvas "height" (str h))
                        ;; Optionally restart the animation or redraw at final-max-iter:
                        (draw-full ctx final-max-iter))))

   ;; 6.1: Set the drawing buffer size to the full viewport
   (.setAttribute canvas "width"  (str w))
   (.setAttribute canvas "height" (str h))
   ;; 6.2: Use CSS to force the canvas to cover the entire screen,
   ;;      no margins or scrollbars.
   (set! (.. canvas -style -position) "fixed")
   (set! (.. canvas -style -top)      "0")
   (set! (.. canvas -style -left)     "0")
   (set! (.. canvas -style -margin)   "0")
   (set! (.. canvas -style -padding)  "0")
   (set! (.. canvas -style -display)  "block")
   ;; If you want to prevent any scrollbars on <body>:
   (set! (.. js/document -body -style -margin)  "0")
   (set! (.. js/document -body -style -overflow) "hidden")
   ;; 6.3: Start the animation
   (animate ctx)))

 ;; Hook `init` into window load
 ;(.addEventListener js/window "load" init)

(.addEventListener js/window "resize"
                   (fn []
                    (let [w (.-innerWidth js/window)
                          h (.-innerHeight js/window)]
                     (.setAttribute canvas "width"  (str w))
                     (.setAttribute canvas "height" (str h))
                     ;; Optionally restart the animation or redraw at final-max-iter:
                     (draw-full ctx final-max-iter))))
