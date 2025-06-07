(ns unit-converter.ui
 (:require
  [unit-converter.core :as uc]))

(defn parse-number [s]
 (let [v (js/parseFloat s)]
  (if (js/isNaN v) nil v)))

(defn format-result
 "If x is an integer, show no decimals; otherwise show n decimals."
 [x decimals]
 (if (== (js/Math.floor x) x)
  (str x)
  (str (.toFixed x decimals))))

(defn init-length-converter []
 (let [val-input   (js/document.getElementById "length-value")
       from-sel    (js/document.getElementById "length-from")
       to-sel      (js/document.getElementById "length-to")
       result-span (js/document.getElementById "length-result")
       compute-fn  (fn []
                    (let [v    (parse-number (.-value val-input))
                          fsrc (.-value from-sel)
                          tdst (.-value to-sel)]
                     (if (nil? v)
                      (set! (.-textContent result-span) "—")
                      (try
                       (let [out (uc/convert-length v fsrc tdst)]
                        (set! (.-textContent result-span)
                              (format-result out 6)))
                       (catch :default _
                        (set! (.-textContent result-span) "Error"))))))]
  ;; Whenever the user types or changes “from”/“to,” recompute instantly:
  (.addEventListener val-input "input"   (fn [_] (compute-fn)))
  (.addEventListener from-sel "change"   (fn [_] (compute-fn)))
  (.addEventListener to-sel   "change"   (fn [_] (compute-fn)))
  ;; Optionally, run once on init so there's no “—” if the defaults already match:
  (compute-fn)))

(defn init-weight-converter []
 (let [val-input   (js/document.getElementById "weight-value")
       from-sel    (js/document.getElementById "weight-from")
       to-sel      (js/document.getElementById "weight-to")
       result-span (js/document.getElementById "weight-result")
       compute-fn  (fn []
                    (let [v    (parse-number (.-value val-input))
                          fsrc (.-value from-sel)
                          tdst (.-value to-sel)]
                     (if (nil? v)
                      (set! (.-textContent result-span) "—")
                      (try
                       (let [out (uc/convert-weight v fsrc tdst)]
                        (set! (.-textContent result-span)
                              (format-result out 6)))
                       (catch :default _
                        (set! (.-textContent result-span) "Error"))))))]
  (.addEventListener val-input "input"   (fn [_] (compute-fn)))
  (.addEventListener from-sel "change"   (fn [_] (compute-fn)))
  (.addEventListener to-sel   "change"   (fn [_] (compute-fn)))
  (compute-fn)))

(defn init-temp-converter []
 (let [val-input   (js/document.getElementById "temp-value")
       from-sel    (js/document.getElementById "temp-from")
       to-sel      (js/document.getElementById "temp-to")
       result-span (js/document.getElementById "temp-result")
       compute-fn  (fn []
                    (let [v    (parse-number (.-value val-input))
                          fsrc (.-value from-sel)
                          tdst (.-value to-sel)]
                     (if (nil? v)
                      (set! (.-textContent result-span) "—")
                      (try
                       (let [out (uc/convert-temp v fsrc tdst)]
                        ;; Show only two decimals for temperature
                        (set! (.-textContent result-span)
                              (format-result out 2)))
                       (catch :default _
                        (set! (.-textContent result-span) "Error"))))))]
  (.addEventListener val-input "input"   (fn [_] (compute-fn)))
  (.addEventListener from-sel "change"   (fn [_] (compute-fn)))
  (.addEventListener to-sel   "change"   (fn [_] (compute-fn)))
  (compute-fn)))

(defn ^:export init []
 ;; Wire up all three converters on page load:
 (init-length-converter)
 (init-weight-converter)
 (init-temp-converter))

;; Run `init` once the window has loaded
(set! (.-onload js/window) init)
