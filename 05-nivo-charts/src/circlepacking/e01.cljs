(ns circlepacking.e01
  (:require
    [reagent.core :as reagent]
    [reagent.dom :as rd]
    ["@nivo/circle-packing" :refer [ResponsiveCirclePacking]]))

;; Sample hierarchical data
(def circle-data
  {:name "Root"
   :children [{:name "Electronics" :value 320}
              {:name "Furniture" :value 210}
              {:name "Clothing"
               :children [{:name "Men" :value 200}
                          {:name "Women" :value 180}]}
              {:name "Toys" :value 150}
              {:name "Books" :value 180}]})

;; Convert data to JS format
(defn js-circle-data [] (clj->js circle-data))

;; Adapt Nivo component for Reagent
(def nivo-circle (reagent/adapt-react-class ResponsiveCirclePacking))

;; Custom tooltip component
(defn custom-tooltip [point]
  (let [data (js->clj (.-data point) :keywordize-keys true)]
    [:div {:style {:background "white" :padding "5px" :border "1px solid #ccc"}}
     [:strong (:name data)] [:br]
     "Value: " [:span {:style {:color "#ff6600"}} (:value data)]]))

;; CirclePacking chart component
(defn circle-packing-chart []
  [nivo-circle
   {:data (js-circle-data)
    :margin {:top 50 :right 50 :bottom 50 :left 50}
    :colors {:scheme "nivo"} ;; Color scheme
    :identity "name"
    :value "value"
    :padding 4
    :enableLabels true
    :tooltip (reagent/reactify-component custom-tooltip)
    :onClick #(js/console.log "Clicked on: " (.-data %))}])

;; Main UI component
(defn main-panel []
  [:div
   [:h2 "Circle Packing Visualization (Nivo)"]
   [:div {:style {:height "500px"}} [circle-packing-chart]]])

;; Mount the UI
(defn ^:export init []
  (rd/render [main-panel]
                  (.getElementById js/document "app")))

