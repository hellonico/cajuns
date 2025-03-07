(ns abstract-art.core
  (:require
    [reagent.core :as r]
    [reagent.dom :as rd]
    ["recharts" :refer [LineChart XAxis YAxis CartesianGrid Line]]))

;(comment
(defonce data (r/atom [{:name "Page A" :uv 4000 :pv 2400}
                       {:name "Page B" :uv 3000 :pv 1398}
                       {:name "Page C" :uv 2000 :pv 9800}
                       {:name "Page D" :uv 2780 :pv 3908}
                       {:name "Page E" :uv 1890 :pv 4800}
                       {:name "Page F" :uv 2390 :pv 3800}
                       {:name "Page G" :uv 3490 :pv 4300}]))

;(defn update-data []
;  (js/setInterval
;    (fn []
;      ;(println @data)
;      (swap! data (fn [current-data]
;                    (vec (map (fn [d] (assoc d :uv (+ (:uv d) (- (rand-int 200) 100))
;                                               :pv (+ (:pv d) (- (rand-int 200) 100))))
;                              current-data)))))
;    2000))
;
(defn update-data []
  (js/setInterval
    (fn []
      (swap!
        data
        (fn [current-data]
          (mapv (fn [d] (assoc d :uv (+ (:uv d) (- (rand-int 1000) 100))
                                 :pv (+ (:pv d) (- (rand-int 1000) 100))))
                current-data))))
    2000))

(defn chart []
  [:> LineChart {:width 500 :height 300 :data @data}
   [:> XAxis {:dataKey "name"}]
   [:> YAxis]
   [:> CartesianGrid {:stroke "#eee" :strokeDasharray "5 5"}]
   [:> Line {:type "monotone" :dataKey "uv" :stroke "#8884d8"}]
   [:> Line {:type "monotone" :dataKey "pv" :stroke "#82ca9d"}]])

;; Function to add a new point to the data
;(defn add-data-point! []
;  (let [new-point {:name (str "Page " (char (+ 65 (count @data)))) ; Generate a new name (e.g., "Page C")
;                   :uv (rand-int 5000) ; Random UV value
;                   :pv (rand-int 5000)}] ; Random PV value
;    (swap! data conj new-point))) ; Add th
;
;;; Main page component
;(defn home-page []
;  [:div
;   [chart]
;   [:button {:on-click add-data-point!}
;    "Add Data Point"]])

(defn init []
  (update-data)
  (rd/render
    [chart]
    (.getElementById js/document "app")))
;)

;(defn -main [] (init))
(set! (.-onload js/window) init)
;
;(comment
;(defonce data [{:name "Page A" :uv 4000 :pv 2400}
;                       {:name "Page B" :uv 3000 :pv 1398}
;                       {:name "Page C" :uv 2000 :pv 9800}
;                       {:name "Page D" :uv 2780 :pv 3908}
;                       {:name "Page E" :uv 1890 :pv 4800}
;                       {:name "Page F" :uv 2390 :pv 3800}
;                       {:name "Page G" :uv 3490 :pv 4300}])
;
;(defn chart []
;  [:> LineChart {:width 500 :height 300 :data data}
;   [:> XAxis {:dataKey "name"}]
;   [:> YAxis]
;   [:> CartesianGrid {:stroke "#eee" :strokeDasharray "5 5"}]
;   [:> Line {:type "monotone" :dataKey "uv" :stroke "#8814d8"}]
;   [:> Line {:type "monotone" :dataKey "pv" :stroke "#82ca9d"}]])
;
;(defn init []
;  (rd/render
;    [chart]
;    (.getElementById js/document "app"))))
