(ns chord.e01
  (:require
    [reagent.core :as reagent]
    [reagent.dom :as rd]
    ["@nivo/chord" :refer [ResponsiveChord]]))

(def matrix [
             [1064 21 1508 724 538]
             [0 4 398 1845 1532]
             [8 989 61 347 1931]
             [164 307 14 164 534]
             [50 1117 406 349 987]
             ])

(def labels [ "John" "Raoul" "Jane" "Marcel" "Ibrahim" ])

(def nivo-chord
  (reagent/adapt-react-class ResponsiveChord))

(defn custom-tooltip [point]
  (let [source (.-source point)
        target (.-target point)]
    [:div {:style {:background "white" :padding "5px" :border "1px solid #ccc"}}
     [:strong (str (.-id source) " → " (.-id target))] [:br]
     "Value: " [:span {:style {:color "#ff6600"}} (.-value point)]]))

(defn chord-chart []
  [nivo-chord
   {:data              matrix
    :keys              labels
    :margin            {:top 50 :right 50 :bottom 50 :left 50}
    :innerRadiusRatio  0.9
    :pad-angle          0.02
    :colors            {:scheme "category10"}
    :arc-opacity        0.7
    :arcBorderColor    {:from "color" :modifiers [["darker" 0.6]]}
    :ribbonOpacity     0.7
    :ribbonBorderColor {:from "color" :modifiers [["darker" 0.6]]}
    :enableLabel       true
    :labelTextColor    {:from "color" :modifiers [["darker" 1.2]]}
    :tooltip           (reagent/reactify-component custom-tooltip)
    :onClick           #(js/console.log "Clicked on: " (.-source %))}])

(defn main-panel []
  [:div
   [:h2 "Chord Diagram Visualization (Nivo)"]
   [:div {:style {:height "500px"}} [chord-chart]]])

(defn ^:export init []
  (rd/render [main-panel]
             (.getElementById js/document "app")))
