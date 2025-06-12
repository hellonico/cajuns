(ns my-neural-network.core
    (:require [reagent.core :as reagent][reagent.dom :as rd]
     [cljs.math :refer [pow]]))

(def learning-rate 0.01)

(defn sigmoid [x]
      (/ 1 (+ 1 (Math/exp (- x)))))

(defn sigmoid-derivative [x]
      (* x (- 1 x)))

(defn init-weights [input-nodes hidden-nodes output-nodes]
      {:weights-input-hidden (->> (repeatedly (* input-nodes hidden-nodes) #(- (rand 2) 1))
                                  (partition hidden-nodes))
       :weights-hidden-output (->> (repeatedly (* hidden-nodes output-nodes) #(- (rand 2) 1))
                                   (partition output-nodes))})

(defn forward-propagation [input weights]
      (mapv (fn [row]
                (sigmoid (reduce + (map * input row))))
            weights))

(defn backward-propagation [input hidden output target weights-input-hidden weights-hidden-output]
      (let [output-error (- target output)
            output-delta (* output-error (sigmoid-derivative output))
            hidden-error (mapv (fn [w] (* output-delta w)) weights-hidden-output)
            hidden-delta (mapv (fn [h e] (* h (- 1 h) e)) hidden hidden-error)]
           {:weights-hidden-output (mapv (fn [w h] (+ w (* learning-rate output-delta h)))
                                         weights-hidden-output hidden)
            :weights-input-hidden (mapv (fn [w i] (+ w (* learning-rate (first hidden-delta) i)))
                                        weights-input-hidden input)}))

(defn train [input target weights]
      (let [hidden (forward-propagation input (:weights-input-hidden weights))
            output (forward-propagation hidden (:weights-hidden-output weights))
            new-weights (backward-propagation input hidden output target
                                              (:weights-input-hidden weights)
                                              (:weights-hidden-output weights))]
           new-weights))

(defn predict [input weights]
      (let [hidden (forward-propagation input (:weights-input-hidden weights))
            output (forward-propagation hidden (:weights-hidden-output weights))]
           output))

(defn home []
 (let [weights (reagent/atom (init-weights 2 4 1))
       input (reagent/atom [1 1])
       target 3]
  (fn []
   [:div
    [:h1 "Neural Network Training"]
    [:p "Input: " (pr-str @input)]
    [:p "Target: " target]
    [:p "Prediction: " (pr-str (predict @input @weights))]
    [:button {:on-click #(swap! weights train @input target)} "Train"]
    [:button {:on-click #(reset! input [(rand 2) (rand 2)])} "New Input"]])))

(defn ^:export init []
 (rd/render [home] (js/document.getElementById "app")))