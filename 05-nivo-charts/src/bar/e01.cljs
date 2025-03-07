(ns bar.e01
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

;; Bar chart component
(defn sales-chart []
  [nivo-bar
   {:data (js-sales-data)
    :keys ["sales"]
    :indexBy "category"
    :margin {:top 50 :right 50 :bottom 50 :left 60}
    :padding 0.3
    :colors {:scheme "nivo"}
    :axisLeft {:tickSize 5 :tickPadding 5 :tickRotation 0}
    :axisBottom {:tickSize 5 :tickPadding 5 :tickRotation 0}
    :enableLabel true
    :animate true}])

;; Main UI component
(defn main-panel []
  [:div
   [:h2 "Sales Data Visualization (Nivo)"]
   [:div {:style {:height "400px"}} [sales-chart]]])

;; Mount the UI
(defn ^:export init []
  (rd/render [main-panel]
                  (.getElementById js/document "app")))