(ns abstract-art.core
  (:require
    [js.core]
    [react.path-drawing]
    [reagent.dom :as rd]))
;
;(defn app []
;  [:div
;   [js.core/animated-box]])

(defn ^:export init []
  (rd/render
     [js.core/animated-box]
    ;[react.path-drawing/app]
    (js/document.getElementById "app")))

(comment

  (require '[reagent.dom :as rd])
  (require '[react.path-drawing])
  (rd/render [react.path-drawing/app] (js/document.getElementById "app"))

  (rd/unmount-component-at-node (js/document.getElementById "app"))

  (require '[abstract-art.core])
  (rd/render [abstract-art.core/app] (js/document.getElementById "app"))


  )
