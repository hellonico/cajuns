(ns abstract-art.core
 (:require [reagent.core :as r]
           [reagent.dom :as rd]))

;; State for form data and errors
(defonce form-data (r/atom {:name "" :email "" :message ""}))
(defonce errors (r/atom {}))

;; Validation function
(defn validate-form []
 (reset! errors {})
 (let [{:keys [name email message]} @form-data]
  (when (empty? name) (swap! errors assoc :name "Name is required"))
  (when (empty? email) (swap! errors assoc :email "Email is required"))
  (when (and (not (empty? email)) (not (re-matches #"^[\w._%+-]+@[\w.-]+\.[a-zA-Z]{2,}$" email)))
   (swap! errors assoc :email "Invalid email format"))
  (when (empty? message) (swap! errors assoc :message "Message cannot be empty"))
  (empty? @errors)))

;; Submit handler
(defn handle-submit [event]
 (.preventDefault event)
 (when (validate-form)
  (js/alert "Form submitted successfully!")))

;; Input component
(defn input-field [label key type]
 [:div.field
  [:label label]
  [:input {:type      type
           :value     (@form-data key)
           :on-change #(swap! form-data assoc key (-> % .-target .-value))}]
  (when-let [err (@errors key)] [:p.error err])])

;; Textarea component
(defn textarea-field [label key]
 [:div.field
  [:label label]
  [:textarea {:value     (@form-data key)
              :on-change #(swap! form-data assoc key (-> % .-target .-value))}]
  (when-let [err (@errors key)] [:p.error err])])

;; Form component
(defn form-component []
 [:div.container
  [:h2 "Contact Us"]
  [:form {:on-submit handle-submit}
   [input-field "Name" :name "text"]
   [input-field "Email" :email "email"]
   [textarea-field "Message" :message]
   [:button "Submit"]]])

(defn init []
 (rd/render [form-component] (js/document.getElementById "app")))
