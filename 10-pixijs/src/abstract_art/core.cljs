(ns abstract-art.core
 (:require ["pixi.js" :as pixi]))

(defn start []
 (let [app (pixi/Application.)]                             ;; Create application instance
  (-> (.init app (clj->js {:backgroundAlpha 0 :resizeTo js/window})) ;; Ensure initialization
      (.then
       (fn []
        ;; Append the application canvas to the document body
        (.appendChild js/document.body (.-canvas app))

        ;; Load the bunny texture
        (-> (.load pixi/Assets "https://pixijs.com/assets/bunny.png")
            (.then (fn [texture]
                    (let [bunny (pixi/Sprite. texture)]
                     ;; Center the sprite's anchor point
                     (.set (.-anchor bunny) 0.5)

                     ;; Move the sprite to the center of the screen
                     (set! (.-x bunny) (/ (.-width (.-screen app)) 2))
                     (set! (.-y bunny) (/ (.-height (.-screen app)) 2))

                     ;; Add the bunny to the stage
                     (.addChild (.-stage app) bunny)

                     ;; Listen for animate update
                     (.add (.-ticker app) (fn []
                                           (set! (.-rotation bunny) (+ (.-rotation bunny) 0.1)))))))))))))


(defn ^:export init []
 (start))