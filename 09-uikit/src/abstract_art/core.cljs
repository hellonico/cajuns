(ns abstract-art.core
 (:require
  [reagent.core :as reagent]
  [reagent.dom :as rd]
  [ajax.core :refer [GET POST]]))

;; Atom to hold the players' data
(defonce players (reagent/atom []))

;; Function to parse CSV text using PapaParse
(defn parse-csv [csv-text]
 (.parse js/Papa csv-text
         (clj->js {:header         true
                   :skipEmptyLines true
                   :complete       (fn [result]
                                    (reset! players (js->clj (.-data result) :keywordize-keys true)))})))

;; Fetch CSV file and parse it
(defn fetch-players [url]
 (GET url
      {:handler       parse-csv
       :error-handler #(js/console.error "Failed to load CSV" %)}))

;; UI for displaying a player card
(defn player-card [player]
 [:div {:class "uk-card uk-card-default uk-card-body uk-padding uk-box-shadow-medium"}
  [:h3 {:class "uk-card-title uk-margin-small-bottom"} (:name player)]
  [:p {:class "uk-text-meta uk-margin-remove-top uk-margin-small-bottom"}
   [:strong "Country: "] (:country player)]
  [:p {:class "uk-margin-small-bottom"} (:bio player)]])

;; UI to display all players' cards
(defn player-cards []
 [:div {:class "uk-container"}
  [:div {:class "uk-grid uk-grid-small uk-child-width-1-1 uk-child-width-1-2@s uk-child-width-1-3@m uk-child-width-1-4@l", :uk-grid true}
   (for [player @players]
    ^{:key (:name player)}
    [:div [player-card player]])]])

(defn mount-root []
 (rd/render [player-cards] (.getElementById js/document "app")))

(defn ^:export init []
 (fetch-players "players.csv")
 (mount-root))
