;; src/cljs/todo/core.cljs
(ns todo.core
 (:require
  [reagent.dom :as rd]
  [re-frame.core :as re-frame]
  [clojure.string :as str]))

;; ----------------------------------------------------------------------------
;; 1) Initial app-db
;; ----------------------------------------------------------------------------
(def default-db
 {:todos    []    ;; vector of {:id <number> :text <string> :done? <boolean> :removing? <boolean>}
  :new-todo ""})

;; ----------------------------------------------------------------------------
;; 2) Event Handlers
;; ----------------------------------------------------------------------------

;; Initialize the DB
(re-frame/reg-event-db
 :initialize-db
 (fn [_ _]
  default-db))

;; Update the :new-todo string as the user types
(re-frame/reg-event-db
 :set-new-todo
 (fn [db [_ text]]
  (assoc db :new-todo text)))

;; Add a new todo (with :removing? false)
(re-frame/reg-event-db
 :add-todo
 (fn [db _]
  (let [txt (str/trim (:new-todo db))]
   (if (str/blank? txt)
    db
    (let [new-id    (if (empty? (:todos db))
                     1
                     (inc (apply max (map :id (:todos db)))))
          new-todo {:id        new-id
                    :text      txt
                    :done?     false
                    :removing? false}]
     (-> db
         (update :todos conj new-todo)
         (assoc :new-todo "")))))))

;; Toggle :done? on a todo by id
(re-frame/reg-event-db
 :toggle-todo
 (fn [db [_ id]]
  (update db :todos
          (fn [todos]
           (mapv (fn [todo]
                  (if (= (:id todo) id)
                   (update todo :done? not)
                   todo))
                 todos)))))

;; Immediately delete (this will actually run after the 600ms delay by :mark-removing fx)
(re-frame/reg-event-db
 :delete-todo
 (fn [db [_ id]]
  (update db :todos
          (fn [todos]
           (vec (remove #(= (:id %) id) todos))))))

;; FX: schedule a JS timeout to dispatch :delete-todo after delay
(re-frame/reg-fx
 :delayed-delete
 (fn [[id delay-ms]]
  (js/setTimeout
   #(re-frame/dispatch [:delete-todo id])
   delay-ms)))

;; Mark a todo as removing? true, then schedule a delayed delete after 600ms
(re-frame/reg-event-fx
 :mark-removing
 (fn [{:keys [db]} [_ id]]
  {:db (update db :todos
               (fn [todos]
                (mapv (fn [todo]
                       (if (= (:id todo) id)
                        (assoc todo :removing? true)
                        todo))
                      todos)))
   ;; After 600ms, dispatch :delete-todo
   :delayed-delete [id 600]}))

;; ----------------------------------------------------------------------------
;; 3) Subscriptions
;; ----------------------------------------------------------------------------

(re-frame/reg-sub
 :new-todo
 (fn [db _]
  (:new-todo db)))

(re-frame/reg-sub
 :todos
 (fn [db _]
  (:todos db)))

;; ----------------------------------------------------------------------------
;; 4) Views (Reagent components)
;; ----------------------------------------------------------------------------

(defn todo-input []
 (let [new-txt (re-frame/subscribe [:new-todo])]
  (fn []
   [:div {:class "input-group"}
    [:input {:type        "text"
             :placeholder "What needs to be done?"
             :value       @new-txt
             :on-change   #(re-frame/dispatch [:set-new-todo (-> % .-target .-value)])
             :on-key-down (fn [e]
                           (when (= (.-key e) "Enter")
                            (re-frame/dispatch [:add-todo])))}]
    [:button {:class    "add-btn"
              :on-click #(re-frame/dispatch [:add-todo])}
     "Add"]])))

(defn todo-item [{:keys [id text done? removing?]}]
 ;; The <li> will pick up class "removing" when flagged
 [:li {:class (when removing? "removing")}
  [:input {:type      "checkbox"
           :checked   done?
           :on-change #(re-frame/dispatch [:toggle-todo id])}]
  [:span {:style {:margin-left "1rem"}} text]
  ;; “×” instead of “Delete”; aria-label helps screen-readers
  [:button {:class       "delete-btn"
            :aria-label  "Delete todo"
            :on-click    #(re-frame/dispatch [:mark-removing id])}
   "×"]])


(defn todo-list []
 (let [todos (re-frame/subscribe [:todos])]
  (fn []
   [:ul {:style {:list-style "none" :padding 0}}
    (for [t @todos]
     ^{:key (:id t)} [todo-item t])])))

(defn todo-app []
 [:div {:style {:max-width   "600px"
                :margin       "2em auto"
                :font-family "'Lato', sans-serif"}}
  [:h2 "Todo List"]
  [todo-input]
  [todo-list]])

;; ----------------------------------------------------------------------------
;; 5) Mount / Initialize
;; ----------------------------------------------------------------------------

(defn ^:export init []
 ;; 1) Reset app-db
 (re-frame/dispatch-sync [:initialize-db])
 ;; 2) Render into <div id="app">
 (rd/render [todo-app]
                 (.getElementById js/document "app")))
