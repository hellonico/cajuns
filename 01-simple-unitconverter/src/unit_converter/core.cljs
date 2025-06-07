(ns unit-converter.core
 (:require [clojure.string :as str]))

;; ----------------------------
;; LENGTH CONVERSION
;; ----------------------------
(def length-factors
 {"m"  1.0
  "km" 1000.0
  "cm" 0.01
  "mm" 0.001
  "mi" 1609.344
  "yd" 0.9144
  "ft" 0.3048
  "in" 0.0254
  "nm" 1.852e+3})

(defn convert-length
 [value from-unit to-unit]
 (let [fu (str/lower-case from-unit)
       tu (str/lower-case to-unit)
       f-from (get length-factors fu)
       f-to   (get length-factors tu)]
  (cond
   (nil? f-from)
   (throw (ex-info (str "Unsupported length unit (from): " fu) {}))

   (nil? f-to)
   (throw (ex-info (str "Unsupported length unit (to): " tu) {}))

   :else
   (let [in-m (* value f-from)]
    (/ in-m f-to)))))

;; ----------------------------
;; WEIGHT CONVERSION
;; ----------------------------
(def weight-factors
 {"kg" 1.0
  "g"  0.001
  "mg" 0.000001
  "lb" 0.45359237
  "oz" 0.0283495231
  "st" 6.35029318
  "t"  1000.0})

(defn convert-weight
 [value from-unit to-unit]
 (let [fu (str/lower-case from-unit)
       tu (str/lower-case to-unit)
       f-from (get weight-factors fu)
       f-to   (get weight-factors tu)]
  (cond
   (nil? f-from)
   (throw (ex-info (str "Unsupported weight unit (from): " fu) {}))

   (nil? f-to)
   (throw (ex-info (str "Unsupported weight unit (to): " tu) {}))

   :else
   (let [in-kg (* value f-from)]
    (/ in-kg f-to)))))

;; ----------------------------
;; TEMPERATURE CONVERSION
;; ----------------------------
(defn c->f [c]
 ;; + ((c * 9) / 5) + 32
 (+ (/ (* c 9) 5) 32))

(defn c->k [c]
 (+ c 273.15))

(defn f->c [f]
 ;; (f - 32) * (5/9)
 (* (- f 32) (/ 5 9)))

(defn f->k [f]
 (c->k (f->c f)))

(defn k->c [k]
 (- k 273.15))

(defn k->f [k]
 (c->f (k->c k)))

(defn convert-temp
 [value from-unit to-unit]
 (let [fu (str/upper-case from-unit)
       tu (str/upper-case to-unit)]
  (cond
   (and (= fu "C") (= tu "F")) (c->f value)
   (and (= fu "C") (= tu "K")) (c->k value)
   (and (= fu "F") (= tu "C")) (f->c value)
   (and (= fu "F") (= tu "K")) (f->k value)
   (and (= fu "K") (= tu "C")) (k->c value)
   (and (= fu "K") (= tu "F")) (k->f value)
   (= fu tu)                   value
   :else
   (throw (ex-info (str "Unsupported temp conversion: " fu " → " tu) {})))))
