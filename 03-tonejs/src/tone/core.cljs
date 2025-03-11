(ns tone.core
 (:require
  [tone.visual.core :as tvc]
  [tone.simple.core :as tsc]))


(defn ^:export init []
 (js/console.log "Initializing music player...")
 (tsc/play-sound)
 )
