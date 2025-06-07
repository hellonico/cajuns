(ns bar.e02
  (:require
    [reagent.core :as reagent]
    [reagent.dom :as rd]
    ["@nivo/bar" :refer [ResponsiveBar]]))

;; Sample sales data
(def sales-data
  [{:category "Electronics" :sales 320}
   {:category "Furniture" :sales 210}
   {:category "Clothing" :sales 400}
   {:category "Toys" :sales 150}
   {:category "Books" :sales 180}])

;; Convert ClojureScript data to JS format
(defn js-sales-data []
  (clj->js (map #(assoc {} "category" (:category %) "sales" (:sales %)) sales-data)))

;; Adapt Nivo component for Reagent
(def nivo-bar (reagent/adapt-react-class ResponsiveBar))

;; Custom tooltip component
(defn custom-tooltip [point]
  (let [data (js->clj (.-data point) :keywordize-keys true)]
    [:div {:style {:background "white" :padding "5px" :border "1px solid #ccc"}}
     [:strong (:category data)] [:br]
     "Sales: " [:span {:style {:color "#ff6600"}} (:sales data)]]))

;; Bar chart component
(defn sales-chart []
  [nivo-bar
   {:data (js-sales-data)
    :keys ["sales"]
    :indexBy "category"
    :margin {:top 50 :right 50 :bottom 50 :left 60}
    :padding 0.3
    :colors {:scheme "red_yellow_blue"}  ;; Custom color scheme
    :axisLeft {:tickSize 5 :tickPadding 5 :tickRotation 0}
    :axisBottom {:tickSize 5 :tickPadding 5 :tickRotation 0}
    :enableLabel true
    :animate true
    :tooltip (reagent/reactify-component custom-tooltip)  ;; Custom tooltip
    :onClick #(js/console.log "Clicked on: " (.-data %))  ;; Click interaction
    :theme {:tooltip {:container {:fontSize 14}}}
    :defs [{:id "gradient" :type "linearGradient" :colors [{:offset 0 :color "#ff6600"} {:offset 100 :color "#ffcc00"}]}]
    :fill [{:match {:id "sales"} :id "gradient"}]}])

;; Main UI component
(defn main-panel []
  [:div
   [:h2 "Sales Data Visualization (Nivo)"]
   [:div {:style {:height "400px"}} [sales-chart]]])

;; Mount the UI
(defn ^:export init []
  (rd/render [main-panel]
                  (.getElementById js/document "app")))
