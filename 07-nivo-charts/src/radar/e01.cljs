(ns radar.e01
 (:require
  [reagent.core :as reagent]
  [reagent.dom :as rd]
  ["@nivo/radar" :refer [ResponsiveRadar]]))

(def radar-data
 [{:taste "Sweet" :A 70 :B 50 :C 80}
  {:taste "Sour" :A 30 :B 80 :C 40}
  {:taste "Bitter" :A 50 :B 30 :C 70}
  {:taste "Salty" :A 90 :B 40 :C 60}
  {:taste "Umami" :A 60 :B 90 :C 50}])

(def radar-keys ["A" "B" "C"])

(def nivo-radar (reagent/adapt-react-class ResponsiveRadar))

(defn custom-tooltip [point]
 (let [point-data (js->clj (.-data point) :keywordize-keys true)]
  [:div {:style {:background "white" :padding "5px" :border "1px solid #ccc"}}
   [:strong (str (:key point-data) ": " (:value point-data))]]))

(defn radar-chart []
 [nivo-radar
  {:data            (clj->js radar-data)
   :keys            (clj->js radar-keys)
   :indexBy         "taste"
   :margin          {:top 50 :right 50 :bottom 50 :left 50}
   :colors          {:scheme "nivo"}
   :borderWidth     2
   :borderColor     {:from "color"}
   :gridLabelOffset 10
   :enableDots      true
   :dotSize         8
   :dotBorderWidth  2
   :dotBorderColor  {:from "color" :modifiers [["darker" 0.3]]}
   :blendMode       "multiply"
   :motionConfig    "wobbly"
   :tooltip         (reagent/reactify-component custom-tooltip)
   :legends        [{
                      :anchor        "top-center"
                      :direction     "column"
                      :translateX    -50
                      :translateY    -40
                      :itemWidth     80
                      :itemHeight    20
                      :itemTextColor "#999"
                      :symbolSize    12
                      :symbolShape   "circle"
                      :effects       [{:on "hover" :style {:itemTextColor "#000"}}]
                      }
                     ]}])

;; Main UI component
(defn main-panel []
 [:div
  [:h2 "Radar Chart with Legend (Nivo)"]
  [:div {:style {:height "500px"}} [radar-chart]]])

;; Mount the UI
(defn ^:export init []
 (rd/render [main-panel]
            (.getElementById js/document "app")))
