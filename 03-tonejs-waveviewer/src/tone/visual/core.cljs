(ns tone.visual.core
 (:require ["howler" :refer [Howl]]
           ["wavesurfer.js" :as WaveSurfer]))

;; Create a sound object using Howler.js
(def my-sound
 (Howl. (clj->js {:src         ["sample-9s.mp3"]
                  :volume      0.5
                  :onload      #(js/console.log "Audio loaded successfully!")
                  :onloaderror #(js/console.error "Failed to load audio.")})))

;; Initialize WaveSurfer.js for audio waveform visualization
(defn init-waveform []
 (let [wavesurfer (WaveSurfer/create #js {:container ".waveform"
                                          :waveColor "green"
                                          :progressColor "orange"
                                          :scrollParent true
                                          :height 200})]

  ;; Load audio for WaveSurfer (visualization only)
  (.load wavesurfer "sample-9s.mp3")

  ;; Sync Howler.js and WaveSurfer.js on seek
  (.on my-sound "seek" #(do
                         (js/console.log "Howler seek:" %)
                         (.seek wavesurfer (.-seek %)))) ;; Sync WaveSurfer with Howler's seek

  ;; Update WaveSurfer's progress while Howler is playing
  (.on my-sound "play" #(do
                         (js/console.log "Howler sound playing")
                         (.play wavesurfer))) ;; Sync WaveSurfer play

  ;; Start playback when WaveSurfer is ready
  (.on wavesurfer "ready" #(do
                            (js/console.log "Waveform loaded!")
                            (.play my-sound))) ;; Play the sound using Howler.js when WaveSurfer is ready

 ;; Pause both WaveSurfer and Howler when paused
 (.on my-sound "pause" #(do
                         (js/console.log "Howler sound paused")
                         (.pause wavesurfer))) ;; Pause WaveSurfer when Howler is paused

 ;; Handle when WaveSurfer seeks manually
 (.on wavesurfer "seek" #(do
                          (js/console.log "WaveSurfer seeking:" %)
                          (.seek my-sound (.-seek %)))))) ;; Sync Howler's seek with WaveSurfer


;; Trigger play sound and waveform visualization
(defn play-sound []
 (init-waveform)) ;; Initialize and load the waveform                   ;; Play the sound