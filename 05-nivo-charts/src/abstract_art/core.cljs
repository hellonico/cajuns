(ns abstract-art.core
  (:require
    [bar.e01]
    [bar.e02]
    [circlepacking.e01]
    [chord.e01]
    [radar.e01]
    ))

;; Mount the UI
(defn ^:export init []
  (radar.e01/init))