(ns abstract-art.core
  (:require
    [reagent.core :as r]
    [reagent.dom :as rd]))

(def filters
  ["none"
   "grayscale(100%)"
   "sepia(100%)"
   "invert(100%)"
   "blur(5px)"
   "contrast(200%)"
   "brightness(200%)"
   "saturate(300%)"
   "hue-rotate(180deg)"
   "drop-shadow(10px 10px 10px black)"
   "contrast(250%) brightness(150%) saturate(200%)"  ;; Comic Book Effect
   "contrast(300%) brightness(50%) saturate(0%)"      ;; Pixelated Effect
   "hue-rotate(200deg) contrast(150%) brightness(120%)"  ;; Cool Blue Tone
   "hue-rotate(-40deg) contrast(150%) brightness(110%)"  ;; Warm Red Tone
   "grayscale(50%) contrast(120%)"
   "sepia(80%) brightness(90%)"
   "invert(50%) saturate(200%)"
   "blur(3px) brightness(110%) contrast(130%)"
   "contrast(400%) brightness(80%)" ;; Extreme Contrast
   "hue-rotate(90deg) saturate(500%)" ;; Neon Effect
   "contrast(120%) sepia(30%) saturate(150%)"
   "hue-rotate(300deg) brightness(130%)"
   "drop-shadow(5px 5px 5px black) contrast(140%)"
   "grayscale(100%) brightness(150%) contrast(180%)" ;; High-contrast B/W
   "invert(100%) contrast(120%) brightness(90%)" ;; X-ray Effect
   "blur(1px) hue-rotate(45deg) contrast(200%)"
   "sepia(60%) hue-rotate(20deg) brightness(110%)"
   "contrast(90%) brightness(120%) saturate(180%)"
   "drop-shadow(15px 15px 15px black) invert(80%)"])


(defn video-feed []
  (let [video-element (r/atom nil)
        current-filter (r/atom 0)]  ;; Store the index of the active filter
    (r/create-class
      {:component-did-mount
       (fn [_]
         (when-let [video @video-element]
           (.then (.getUserMedia js/navigator.mediaDevices (clj->js {:video true}))
                  (fn [stream]
                    (set! (.-srcObject video) stream))
                  (fn [err]
                    (js/console.error "Error accessing camera:" err)))))

       :reagent-render
       (fn []
         [:div {:style {:text-align "center"}}
          [:video {:ref #(reset! video-element %)
                   :autoPlay true
                   :muted true
                   :style {:filter (get filters @current-filter)
                           :width "80%"
                           :height "auto"
                           :border "2px solid black"}}]
          [:br]
          [:button {:on-click #(swap! current-filter (fn[x] (mod (inc x) (count filters))))
                    :style {:margin-top "10px"
                            :padding "8px"
                            :font-size "16px"
                            :cursor "pointer"}}
           "Change Filter"]])})))

(defn ^:export init []
  (rd/render [video-feed] (.getElementById js/document "app")))
