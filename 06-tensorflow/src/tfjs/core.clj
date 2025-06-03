(ns tfjs.core
    (:require ["@tensorflow/tfjs" :as tf]
     ["@tensorflow-models/mobilenet" :as mobilenet]))

(def model (atom nil))

(defn load-model []
      (-> (.load mobilenet)
          (.then (fn [m]
                     (reset! model m)
                     (js/console.log "Model loaded!")))))

(defn classify-image [img-id]
      (let [img (.getElementById js/document img-id)]
           (when @model
                 (-> (.classify @model img)
                     (.then (fn [predictions]
                                (js/console.log "Predictions:" predictions)))))))

(defn init []
      (load-model)
      (.addEventListener js/document "DOMContentLoaded"
                         #(classify-image "input-image")))

(init)
