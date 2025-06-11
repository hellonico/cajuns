(ns sleep.core
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]))
; https://www.yogaeasy.com/artikel/a-guide-to-sound-healing
;; ---- Configurations ----
;(def moods
;  {:calm        {:color "#00FFFF" :filter-cutoff 800  :base-gain 0.02}
;   :deep        {:color "#4B0082" :filter-cutoff 600  :base-gain 0.015}
;   :bright      {:color "#FFD700" :filter-cutoff 1200 :base-gain 0.03}
;   :love        {:color "#FF69B4" :filter-cutoff 17000  :base-gain 0.018}
;   :sleep  {:color "#2E0854" :filter-cutoff 500  :base-gain 0.012}
;   :brain       {:color "#8A2BE2" :filter-cutoff 77   :base-gain 0.01}})
(def moods
  {:relief       {:label "Pain Relief"
                  :color "#6BAED6"     ;; soft blue
                  :filter-cutoff 174
                  :base-gain 0.015}

   :healing      {:label "Tissue Healing"
                  :color "#74C476"     ;; gentle green
                  :filter-cutoff 285
                  :base-gain 0.018}

   :liberation   {:label "Liberation"
                  :color "#9E9AC8"     ;; lavender
                  :filter-cutoff 396
                  :base-gain 0.02}

   :change       {:label "Facilitate Change"
                  :color "#FDAE6B"     ;; soft orange
                  :filter-cutoff 417
                  :base-gain 0.02}

   :transformation {:label "DNA Repair"
                    :color "#F768A1"   ;; vibrant pink
                    :filter-cutoff 528
                    :base-gain 0.02}

   :connection   {:label "Reconnect Relationships"
                  :color "#FFEDA0"     ;; golden
                  :filter-cutoff 639
                  :base-gain 0.022}

   :expression   {:label "Self Expression"
                  :color "#A1D99B"     ;; mint green
                  :filter-cutoff 741
                  :base-gain 0.02}

   :order        {:label "Spiritual Order"
                  :color "#6A51A3"     ;; deep indigo
                  :filter-cutoff 852
                  :base-gain 0.017}

   :oneness      {:label "Unity"
                  :color "#C994C7"     ;; violet rose
                  :filter-cutoff 963
                  :base-gain 0.016}})


;(def noise-types
;  {:white {:label "White Noise"}
;   :piano {:label "Piano Tone"}})

(defonce app-state
         (r/atom {:page       :menu
                  :mood       :calm
                  :anim-frame-id nil
                  :noise-type :white}))

;; ---- Audio Setup ----
(defonce audio-context (js/AudioContext.))
(defonce audio-nodes   (atom nil))
(defonce analyser      (atom nil))

(defn stop-audio []
  (when-let [tone (:low-tone @audio-nodes)]
    (try (.stop tone) (catch js/Error _)))
  (when-let [nodes @audio-nodes]
    (when-let [src (:source nodes)]
      (try (.stop src) (catch js/Error _)))
    (when-let [oscs (:oscillators nodes)]
      (doseq [o oscs]
        (try (.stop o) (catch js/Error _))))
    (reset! audio-nodes nil)
    (reset! analyser nil)))

(defn init-audio []
  (stop-audio)
  (let [{:keys [noise-type mood]} @app-state
        {:keys [filter-cutoff base-gain]} (get moods mood)]
    (if (= noise-type :white)
      ;; --- Enhanced White Noise Engine ---
      (let [sr      (.-sampleRate audio-context)
            dur     2
            buf     (.createBuffer audio-context 1 (* sr dur) sr)
            data    (.getChannelData buf 0)
            src     (.createBufferSource audio-context)
            gain    (.createGain audio-context)
            an      (.createAnalyser audio-context)
            ;; multiple filters
            filt1   (.createBiquadFilter audio-context)
            filt2   (.createBiquadFilter audio-context)
            filt3   (.createBiquadFilter audio-context)
            ;; LFO
            lfo     (.createOscillator audio-context)
            lfoGain (.createGain audio-context)]

        ;; Fill buffer with white noise
        (dotimes [i (.-length data)]
          (aset data i (- (rand 2) 1)))

        ;; Setup base source
        (set! (.-buffer src) buf)
        (set! (.-loop src) true)

        ;; Filters vary by mood
        (cond
          (= mood :calm)
          (do (set! (.-type filt1) "lowpass")
              (set! (.-frequency filt1) 800))

          (= mood :bright)
          (do (set! (.-type filt1) "lowpass")
              (set! (.-frequency filt1) 1500)
              (set! (.-Q filt1) 0.8))


          (= mood :deep)
          (do (set! (.-type filt1) "bandpass")
              (set! (.-frequency filt1) 400))

          (= mood :love)
          (do (set! (.-type filt1) "bandpass")
              (set! (.-frequency filt1) 500))

          (= mood :deep-sleep)
          (do (set! (.-type filt1) "lowpass")
              (set! (.-frequency filt1) 300))

          (= mood :brain)
          (do (set! (.-type filt1) "bandpass")
              (set! (.-frequency filt1) 77)))

        ;; Additional textures
        (set! (.-type filt2) "highpass")
        (set! (.-frequency filt2) 60)

        (set! (.-type filt3) "lowpass")
        (set! (.-frequency filt3) 2000)

        ;; Gain setup
        (set! (.-gain gain) base-gain)

        (let [tone     (.createOscillator audio-context)
              toneGain (.createGain audio-context)
              ]
          (set! (.-type tone) "triangle")
          (set! (.-frequency tone)
                (case mood
                  :calm 40
                  :deep 30
                  :bright 50
                  :love 38
                  :deep-sleep 28
                  :brain 33
                  35))
          (set! (.-value (.-gain toneGain)) 0.02)
          (.connect toneGain an)
          (.start tone)
          ;; store in audio-nodes
          (swap! audio-nodes assoc :low-tone tone :low-tone-gain toneGain))

        ;; LFO modulates filter1 cutoff for movement
        (set! (.-type lfo) "sine")
        (set! (.-frequency lfo)
              (case mood
                :calm        0.05
                :deep        0.03
                :bright      0.2
                :love        0.07
                :deep-sleep  0.01
                :brain       0.005
                0.1))
        (set! (.-value (.-gain lfoGain))
              (case mood
                :calm        200
                :deep        150
                :bright      500
                :love        120
                :deep-sleep  80
                :brain       50
                100))
        (.connect lfo lfoGain)
        (.connect lfoGain (.-frequency filt1))
        (.start lfo)

        ;; Analyser
        (set! (.-fftSize an) 256)

        ;; Connect chain: src -> filt1 -> filt2 -> filt3 -> gain -> analyser -> dest
        (.connect src filt1)
        (.connect filt1 filt2)
        (.connect filt2 filt3)
        (.connect filt3 gain)
        (.connect gain an)
        (.connect an (.-destination audio-context))

        ;; Start sound
        (.start src 0)

        ;; Save nodes
        (reset! analyser an)
        (reset! audio-nodes {:source src
                             :filter1 filt1
                             :filter2 filt2
                             :filter3 filt3
                             :gain gain
                             :lfo lfo
                             :lfo-gain lfoGain}))

      ;; --- Piano tone path remains unchanged ---
      (let [freqs (if (= mood :brain) [77] [261.63 329.63 392.00])
            oscs  (mapv (fn [f]
                          (let [o (.createOscillator audio-context)]
                            (set! (.-type o) "sine")
                            (set! (.-frequency o) f)
                            o)) freqs)
            filt  (.createBiquadFilter audio-context)
            gain  (.createGain audio-context)
            an    (.createAnalyser audio-context)]
        (set! (.-type filt) "lowpass")
        (set! (.-value (.-frequency filt)) filter-cutoff)
        (set! (.-value (.-gain gain)) base-gain)
        (set! (.-fftSize an) 256)
        (doseq [o oscs] (.connect o filt))
        (.connect filt gain)
        (.connect gain an)
        (.connect an (.-destination audio-context))
        (doseq [o oscs] (.start o 0))
        (reset! analyser an)
        (reset! audio-nodes {:oscillators oscs :filter filt :gain gain})))))

(defn apply-mood-and-noise []
  (.resume audio-context)
  (init-audio))

;; ---- Wave Data & Visualization ----
(defonce wave-data (atom (vec (repeat 120 0))))

(defonce wave-phase (atom 0))

(defn update-wave-data []
  (when-let [an @analyser]
    (let [arr (js/Float32Array. (.-frequencyBinCount an))]
      (.getFloatFrequencyData an arr)
      (let [fft-energy (->> arr
                            (take 32) ;; focus on lower spectrum
                            (map #(+ 160 %)) ; normalize from [-160, 0] to [0,160]
                            (map #(/ % 160)) ; -> [0,1]
                            (reduce +)
                            (/ 32.0))
            phase-step 0.03
            new-phase (+ @wave-phase phase-step)
            data (mapv (fn [i]
                         (let [x (/ i 60.0) ;; normalized domain
                               base-sine (* 0.6 (js/Math.sin (+ (* x js/Math.PI) new-phase)))
                               fft-variance (* fft-energy (- (rand 2) 1) 0.25)]
                           (+ base-sine fft-variance)))
                       (range 120))]
        (reset! wave-data data)
        (reset! wave-phase new-phase)))))




(defn draw-wave [ctx mood-color]
  (let [cvs  (.-canvas ctx)
        w    (.-width cvs)
        h    (.-height cvs)
        data @wave-data
        base-color mood-color
        amp  (reduce + (map #(js/Math.abs %) data))
        intensity (min 1.0 (/ amp 20.0))]

    ;; Clear canvas with trailing effect
    (set! (.-fillStyle ctx) "rgba(0,0,0,0.1)")
    (.fillRect ctx 0 0 w h)

    ;; Function to draw a single wave layer
    (letfn [(draw-layer [offset scale alpha]
              (let [grad (.createLinearGradient ctx 0 0 w 0)]
                (.addColorStop grad 0 "rgba(0,0,0,0)")
                (.addColorStop grad 0.5 base-color)
                (.addColorStop grad 1 "rgba(0,0,0,0)")

                (set! (.-strokeStyle ctx) grad)
                (set! (.-lineWidth ctx) 1.0)
                (set! (.-shadowColor ctx) (str "rgba(255,255,255," (* intensity alpha) ")"))
                (set! (.-shadowBlur ctx) 10)

                (.beginPath ctx)
                (let [step (/ w (count data))]
                  (doseq [i (range (count data))]
                    (let [x (* i step)
                          y (+ (/ h 2)
                               (* (/ h scale)
                                  (+ (nth data i)
                                     (* 0.1 (js/Math.sin (+ (* 0.2 i) offset))))))]
                      (if (zero? i)
                        (.moveTo ctx x y)
                        (.lineTo ctx x y)))))
                (.stroke ctx)))]

      ;; Draw 3 layered waves
      (draw-layer 0         4 0.4)  ;; main wave (gentle)
      (draw-layer 1.5       6 0.25) ;; background wave (lower amp)
      (draw-layer -1.2      3.5 0.15))) ;; front wave (subtle shimmer)

  ;; Reset shadow
  (set! (.-shadowBlur ctx) 0))




(defonce animating (atom false))
(defn stop-animation []
  (when-let [id (:anim-frame-id @app-state)]
    (js/cancelAnimationFrame id)
    (swap! app-state assoc :anim-frame-id nil)
    (reset! animating false)))

(defn start-animation []
  (when-not @animating
    (reset! animating true)
    (let [c (.getElementById js/document "wave-canvas")]
      (if c
        (let [ctx (.getContext c "2d")
              last-ts (atom 0)]
          (letfn [(animate-frame [ts]
                    (when (> (- ts @last-ts) 60)
                      (reset! last-ts ts)
                      (update-wave-data)
                      (when-let [{:keys [gain]} @audio-nodes]
                        (let [amp (nth @wave-data (quot (count @wave-data) 2))]
                          (set! (.-value gain)
                                (* (max 0 amp)
                                   (:base-gain (get moods (:mood @app-state)))
                                   1.5))))
                      (draw-wave ctx (:color (get moods (:mood @app-state)))))
                    (let [next-id (js/requestAnimationFrame animate-frame)]
                      (swap! app-state assoc :anim-frame-id next-id)))]
            (let [initial-id (js/requestAnimationFrame animate-frame)]
              (swap! app-state assoc :anim-frame-id initial-id)))
          )
        ;; Retry if canvas is not found
        (js/setTimeout start-animation 100)))))


;; ---- UI Components ----
(defn menu-page []
  (let [{:keys [mood noise-type]} @app-state]
    [:div {:style {:display        "flex"
                   :flexDirection  "column"
                   :alignItems     "center"
                   :justifyContent "center"
                   :height         "100vh"
                   :background     "#000"
                   :color          "#fff"}}
     [:h1 "Mood Waves"]
     [:div {:style {:display "flex"
                    :flexDirection "column"
                    :alignItems "center"
                    :justifyContent "center"
                    :marginTop "2rem"
                    :maxWidth "90vw"}}

      ;; Mood Button Grid
      [:div {:style {:display "flex"
                     :flexWrap "wrap"
                     :justifyContent "center"
                     :gap "1rem"
                     :maxWidth "900px"}}
       (for [[m params] (sort-by (comp :filter-cutoff val) moods)]
        ^{:key m}
        [:button {:style (merge {:margin "0.75rem"
                                 :padding "1.0rem 1.5rem"
                                 :border "2px solid rgba(255,255,255,0.5)"
                                 :borderRadius "30px"
                                 :cursor "pointer"
                                 :background (str (:color params) "") ;; translucent
                                 :color "#fff"
                                 :fontSize "1rem"
                                 :fontFamily "'Nunito', sans-serif"
                                 :backdropFilter "blur(6px)"
                                 :transition "all 0.2s ease-in-out"
                                 :textAlign "center"
                                 :minWidth "200px"
                                 :boxShadow (when (= m mood) "0 0 20px #fff")}
                                (when (= m mood) {:transform "scale(1.05)"}))
                  :on-mouse-over
                  #(do
                     (swap! app-state assoc :mood m))
                  :on-click
                  #(do
                     (swap! app-state assoc :mood m)
                     (swap! app-state assoc :noise-type :white)
                     (swap! app-state assoc :page :app)
                     (js/setTimeout apply-mood-and-noise 50)
                     (js/setTimeout start-animation 100))

                  }
         [:div {:style {:fontWeight 500 :fontSize "1.5rem"}} (:label params)]
         [:div {:style {:fontSize "0.8rem" :opacity 0.8}} (str (get-in params [:filter-cutoff]) " Hz")]]
        )]]]))


(defn app-page []
  (let [{:keys [mood]} @app-state
        {:keys [color filter-cutoff label]} (get moods mood)]
    [:div {:style {:position "relative"
                   :background "#000"
                   :width "100vw"
                   :height "100vh"
                   :overflow "hidden"}}
     [:canvas {:id     "wave-canvas"
               :width  (.-innerWidth js/window)
               :height (.-innerHeight js/window)
               :style  {:display "block"
                        :position "absolute"
                        :top "0"
                        :left "0"
                        :width "100%"
                        :height "100%"}}]
     ;; Mood Label Overlay
     [:div {:style {:position "absolute"
                    :top "5vh"
                    :width "100%"
                    :textAlign "center"
                    :color "#fff"
                    :fontFamily "'Nunito', sans-serif"
                    :pointerEvents "none"}}
      [:div {:style {:fontSize "2.5rem"
                     :fontWeight "bold"
                     :textShadow (str "0 0 15px " color ", 0 0 30px " color)
                     :animation "pulseGlow 4s ease-in-out infinite"}}
       (str (clojure.string/upper-case label))]

      [:div {:style {:fontSize "1rem"
                     :marginTop "0.3rem"
                     :opacity 0.8
                     :textShadow (str "0 0 10px " color)}}
       (str filter-cutoff " Hz")]]]))



(defn root []
  (r/create-class
    {:component-did-mount
     (fn []

       (let [font-link (.createElement js/document "link")]
         (set! (.-rel font-link) "stylesheet")
         (set! (.-href font-link)
               "https://fonts.googleapis.com/css2?family=Quicksand:wght@300;400;600&display=swap")
         (.appendChild (.-head js/document) font-link))

       ;; Global body styling
       (let [style-el (.createElement js/document "style")]
         (set! (.-innerHTML style-el)
               (str
                 "body { background-color: #000; color: #fff; margin: 0; font-family: 'Quicksand', sans-serif; }"
                 "@keyframes pulseGlow { "
                 "  0% { opacity: 1; text-shadow: 0 0 10px rgba(255,255,255,0.5); }"
                 " 50% { opacity: 0.6; text-shadow: 0 0 20px rgba(255,255,255,1); }"
                 "100% { opacity: 1; text-shadow: 0 0 10px rgba(255,255,255,0.5); }"
                 "}"))
         (.appendChild (.-head js/document) style-el))



       ;; Escape to stop audio
       (.addEventListener js/document "keydown"
                          (fn [e]
                            (when (= (.-key e) "Escape")
                              (stop-animation)
                              (stop-audio)
                              (reset! animating false)
                              (swap! app-state assoc :page :menu))))

       ;; Scroll to control volume
       (.addEventListener js/document "wheel"
                          (fn [e]
                            (when-let [{:keys [gain]} @audio-nodes]
                              (let [curr (.-value gain)
                                    delta (* (.-deltaY e) -0.005) ;; smaller step
                                    new-gain (-> (+ curr delta)
                                                 (max 0.0)
                                                 (min 1.0))]
                                (set! (.-value gain) new-gain)))))
)

     :reagent-render
     (fn []
       (case (:page @app-state)
         :menu [menu-page]
         :app  [app-page]))}))

;; ---- Entry Point ----
(defn ^:export init []
  (rdom/render [root] (.getElementById js/document "app")))
