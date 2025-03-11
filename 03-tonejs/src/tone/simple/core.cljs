(ns tone.simple.core
 (:require ["howler" :refer [Howl]]
           [cljs.core.async :refer [timeout go-loop]]))

;; Define an atom to hold the volume level
(def volume (atom 0.1))  ;; Start at low volume

;; Create the sound object
(def my-sound
 (new Howl (clj->js
            {:src         ["sample-9s.mp3"]
             :volume      @volume
             :onload      #(js/console.log "Audio loaded successfully!")
             :onloaderror #(js/console.error "Failed to load audio." %)})))

;; Function to gradually increase volume
(defn increase-volume []
 (go-loop []
          (when (< @volume 2.0)  ;; Max volume is 1.0
           (swap! volume + 0.1) ;; Increase volume by 0.1
           (.volume my-sound @volume) ;; Apply new volume
           (js/console.log "Volume:" @volume)
           (<! (timeout 500)) ;; Wait 500ms before increasing again
           (recur)))) ;; Repeat

(defn ^:export play-sound []
 (.play my-sound)
 (increase-volume)) ;; Start increasing volume after play
