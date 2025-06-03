(ns parallax.e02
 (:require [reagent.core :as r]))

(defn parallax-scroll []
 ;; 1) Keep a local atom to track window.scrollY
 (let [pos (r/atom 0)]
  (r/create-class
   {:display-name "parallax-scroll"

    ;; 2) When the component mounts, register a scroll‐handler
    :component-did-mount
    (fn [this]
     (let [handler (fn []
                    (reset! pos (.-scrollY js/window)))]
      (aset this "scrollHandler" handler)
      (.addEventListener js/window "scroll" handler)))

    ;; 3) When the component unmounts, remove that listener
    :component-will-unmount
    (fn [this]
     (let [handler (aget this "scrollHandler")]
      (when handler
       (.removeEventListener js/window "scroll" handler))))

    ;; 4) The render function uses @pos to compute each layer’s offset
    :reagent-render
    (fn []
     (let [y           @pos
           ;; Each factor <1 makes that layer move more slowly than “full scroll”
           bg-offset   (* y 0.3)   ;; background moves at 30% of scroll speed
           mid-offset  (* y 0.6)   ;; mid layer moves at 60%
           fg-offset   (* y 1.0)]  ;; foreground moves at 100%
      [:div
       ;; OUTER CONTAINER: tall enough to force window‐scroll
       {:style {:position   "relative"
                :height     "200vh"   ;; 200% of viewport height → page will scroll
                :background "#111"}}

       ;; BACKGROUND layer (z-index: 1). At y=0, top=0px; as y grows, top = −(y*0.3) px.
       [:div
        {:style {:position        "absolute"
                 :top             (str (- bg-offset) "px")
                 :left            "0"
                 :width           "100%"
                 :height          "100vh"
                 :background      "url('https://images6.alphacoders.com/428/428645.jpg') no-repeat center center"
                 :background-size "cover"
                 :z-index         1}}
        ;; (We leave the div empty because it’s purely a background image.)
        ]

       ;; MID layer (z-index: 2). Semi-transparent red bar. At y=0, top=0px.
       [:div
        {:style {:position   "absolute"
                 :top        (str (- mid-offset) "px")
                 :left       "0"
                 :width      "100%"
                 :height     "100vh"
                 :background "rgba(200, 0, 0, 0.5)"
                 :z-index    2}}
        [:h2
         {:style {:color        "#fff"
                  :font-size    "2rem"
                  :text-align   "center"
                  :margin-top   "40vh"
                  :text-shadow  "0 0 8px rgba(0,0,0,0.75)"}}
         "Mid Layer with some long text"]]

       ;; FOREGROUND layer (z-index: 3). Semi-transparent green bar. At y=0, top=0px.
       [:div
        {:style {:position   "absolute"
                 :top        (str (- fg-offset) "px")
                 :left       "0"
                 :width      "100%"
                 :height     "100vh"
                 :background "rgba(0, 150, 0, 0.7)"
                 :z-index    3}}
        [:h1
         {:style {:color        "#fff"
                  :font-size    "3rem"
                  :text-align   "center"
                  :margin-top   "45vh"
                  :text-shadow  "0 0 12px rgba(0,0,0,0.75)"}}
         "Foreground Foreground Foreground Foreground Foreground Foreground Foreground Foreground Foreground"]]

       ;; OPTIONAL: normal content begins at “100vh” so you can scroll past the hero
       [:div
        {:style {:position   "absolute"
                 :top        "100vh"
                 :left       "0"
                 :width      "100%"
                 :min-height "100vh"
                 :background "#fff"
                 :padding    "2rem"
                 :z-index    0}}
        [:p {:style {:font-size "1.25rem" :color "#333"}}
         "Here’s some plain page content below the parallax‐layers. Scroll to see how each
          layer shifts at a different speed."]]]))})))

;; Then just render it somewhere in your app:
;; [parallax.e02/parallax-scroll]
