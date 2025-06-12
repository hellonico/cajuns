(ns abstract-art.core
 (:require
  [parallax.e01]
  [parallax.e02]
  [reagent.dom :as rd]))

(defn init []
 (rd/render
  ;[parallax.e01/parallax-component]
  [parallax.e02/parallax-scroll]
            (js/document.getElementById "app")))
