(ns js.core
  (:require
    ["motion" :refer [animate]]
    [reagent.core :as r]))

(defn animated-box []
  (r/create-class
    {:component-did-mount
     (fn []
       (animate (js/document.querySelector "#box")
                #js {:x 500 :opacity 1}
                #js {:duration 3}))

     :reagent-render
     (fn []
       [:div {:id    "box"
              :style {:width      "100px"
                      :height     "100px"
                      :background "blue"
                      :opacity    0}}])}))
