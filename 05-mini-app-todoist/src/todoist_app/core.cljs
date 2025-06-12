;; src/todoist_app/core.cljs
(ns todoist-app.core
  (:require
    [reagent.core :as r]
    [reagent.dom :as rd]
    [ajax.core :refer [GET POST json-response-format]]
    [clojure.string :as str]))

;; State
(defonce token (r/atom (js/localStorage.getItem "todoist-token")))
(defonce temp-token (r/atom ""))
(defonce tasks (r/atom []))
(defonce labels (r/atom {}))
(defonce label-filter (r/atom ""))
(defonce date-filter (r/atom "Today"))
(defonce editing-task (r/atom nil))
(defonce edit-content (r/atom ""))

;; Helpers
(defn parse-tags [s]
  (->> (str/split s #",")
       (map str/trim)
       (remove str/blank?)))

(defn pad2 [n]
  (let [s (str n)]
    (if (< n 10)
      (str "0" s)
      s)))

(defn format-date [^js/Date date]
  (let [y (.getFullYear date)
        m (inc (.getMonth date))
        d (.getDate date)]
    (str y "-"
         (pad2 m) "-"
         (pad2 d))))

(defn compute-date-range [opt]
  (let [now (js/Date.)
        start (js/Date.)]
    (case opt
      "Today"
      ;; last 24 hours
      (do
        (.setDate start (dec (.getDate now))))
      ;; other cases unchanged...
      "This Week"
      (let [dow (.getDay now)
            offset (mod (+ dow 6) 7)]
        (.setDate start (- (.getDate now) offset))
        (.setHours start 0 0 0 0)
        ;(.setHours end   23 59 59 999)
        )

      "This Month"
      (do
        (.setDate start 1)
        (.setHours start 0 0 0 0)
        ;(.setHours end   23 59 59 999)
        )

      "Last 3 Months"
      (do
        (.setMonth start (- (.getMonth now) 2))
        (.setDate start 1)
        (.setHours start 0 0 0 0))

      nil

      ;; e.g. "This Week" / "This Month" / etc.
      )
    ;; we only care about :start now
    {:start (format-date start)}))

;; build-filter now prefixes labels with '@' instead of '#'
;; 1) Switch your filter builder to use the `label:` operator instead of “@” or “#”
(defn build-filter []
  (let [labels (parse-tags @label-filter)
        dr (compute-date-range @date-filter)
        label-str (when (seq labels)
                    (->> labels
                         (map #(str "label:" %))
                         (str/join " & ")))
        date-str (when dr
                   (str "created after:" (:start dr)))
        parts (remove nil? [label-str date-str])]
    (str/join " & " parts)))




;; API Calls

(defn fetch-labels! []
  (GET "https://api.todoist.com/rest/v2/labels"
       {:headers         {"Authorization" (str "Bearer " @token)}
        :response-format (json-response-format {:keywords? true})
        :handler         #(reset! labels (into {} (map (juxt :id :name) %)))
        :error-handler   #(js/console.error "Error fetching labels:" %)}))

(defn fetch-tasks! []
  (let [filter-str (build-filter)]
    (js/console.log "Fetching tasks with filter:" filter-str)
    (GET "https://api.todoist.com/rest/v2/tasks"
         {:headers         {"Authorization" (str "Bearer " @token)}
          :params          (when (seq filter-str) {:filter filter-str})
          :response-format (json-response-format {:keywords? true})
          :handler         #(reset! tasks %)
          :error-handler   #(js/console.error "Error fetching tasks:" %)})))

;; Token Handling
(defn save-token! []
  (when (seq @temp-token)
    (js/localStorage.setItem "todoist-token" @temp-token)
    (reset! token @temp-token)
    (fetch-labels!)
    (fetch-tasks!)))


(defn update-task! [id new-content]
  (POST (str "https://api.todoist.com/rest/v2/tasks/" id)
        {:method          "PATCH"
         :headers         {"Authorization" (str "Bearer " @token)
                           "Content-Type"  "application/json"}
         :body            (js/JSON.stringify #js {"content" new-content})
         :response-format (json-response-format {:keywords? true})
         :handler         #(do
                             (js/console.log "Updated task" id)
                             (fetch-tasks!))
         :error-handler   #(js/console.error "Update failed:" %)}))

;; UI Components
(defn token-form []
  [:div
   [:h2 "Enter your Todoist API Token"]
   [:input {:type        "password"
            :placeholder "API Token"
            :value       @temp-token
            :on-change   #(reset! temp-token (.. % -target -value))}]
   [:button {:on-click save-token!} "Save Token"]])


(defn filter-panel []
  [:form {:class     "filter-panel"
          :on-submit (fn [e]
                       (.preventDefault e)
                       (fetch-tasks!))}
   [:input {:type        "text"
            :placeholder "Labels (comma-separated)"
            :value       @label-filter
            :on-change   #(reset! label-filter (.. % -target -value))}]
   [:select {:value     @date-filter
             :on-change #(reset! date-filter (.. % -target -value))}
    (for [opt ["Today" "This Week" "This Month" "Last 3 Months"]]
      ^{:key opt} [:option {:value opt} opt])]
   [:button {:type "submit"} "Apply"]])

(defn task-list []
  [:ul {:class "task-list"}
   (doall
   (for [{:keys [id content created_at due labels url]} @tasks]
     (let [id-str      (str id)
           is-editing? (= id-str @editing-task)]
     ^{:key id-str}
     [:li {:class "task-item"}
      ;; clickable icon only
      [:span.icon
       {:on-click (fn [e]
                    ;; prevent pill-click etc.
                    (.stopPropagation e)
                    (.open js/window url "_blank"))}
       ;; external-link SVG
       [:svg {:xmlns           "http://www.w3.org/2000/svg"
              :width           "16" :height "16"
              :viewBox         "0 0 24 24"
              :fill            "none"
              :stroke          "currentColor"
              :stroke-width    "2"
              :stroke-linecap  "round"
              :stroke-linejoin "round"}
        [:path {:d "M18 13v6a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"}]
        [:polyline {:points "15 3 21 3 21 9"}]
        [:line {:x1 "10" :y1 "14" :x2 "21" :y2 "3"}]]]

      (if is-editing?
        ;; edit mode
        [:input
         {:type        "text"
          :class       "task-edit-input"
          :value       @edit-content            ;; controlled value
          :auto-focus  true
          :on-change   (fn [e]
                         (let [v (.. e -target -value)]
                           (reset! edit-content v)))  ;; update on every keystroke
          :on-key-down (fn [e]
                         (when (= (.-keyCode e) 13)   ;; ENTER
                           (.preventDefault e)
                           (update-task! id @edit-content)
                           (reset! editing-task nil)))
          :on-blur     (fn []
                         (update-task! id @edit-content)
                         (reset! editing-task nil))}]
        ;; read-only mode
        [:div {:class    "task-content"
               :on-click (fn [e]
                           ;(println "edit:" (str/trim id))
                           (reset! editing-task id)
                           (reset! edit-content content))
               }
         [:div {:class "title"} content]
         [:div is-editing?]
         [:div {:class "meta"}
          (when created_at
            (str "Added: " (subs created_at 0 10)))
          (when-let [d (get-in due [:date])]
            (str " • Due: " d))]
         ;; now render the labels array directly:
         (when (seq labels)
           [:div {:class "tags"}
            (for [lbl labels]
              ^{:key lbl}
              [:span.label-pill
               {:on-click (fn [_]
                            (reset! label-filter lbl)
                            (fetch-tasks!))}
               lbl])])])])))])


(defn app []
  (if @token
    [:div
     [filter-panel]
     [task-list]]
    [token-form]))

;; Initialize
(defn ^:export init []
  (rd/render [app] (.getElementById js/document "app"))
  (when @token
    (fetch-labels!)
    (fetch-tasks!)))

;(.addEventListener js/document "DOMContentLoaded" init)
