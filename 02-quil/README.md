# quil setup with deps.edn

1. Create the new shadow-cljs project
```
npx create-cljs-project quil-sample
```

2. Create the file 

```bash
touch src/abstract_art/core.cljs
```

3. add the content

```clojure
(ns abstract-art.core
  (:require [quil.core :as q]
            [quil.middleware :as m]))

(defn setup []
  (q/frame-rate 10)
  (q/color-mode :hsb)
  {:shapes
   (repeatedly
     10
     #(vector (rand (q/width))
              (rand (q/height))
              (rand 255)
              (rand 100)
              (rand 100)))})

(defn draw [{:keys [shapes]}]
  (q/background 0)
  (doseq [[x y h s b] shapes]
    (q/fill h s b)
    (q/ellipse x y (rand 100) (rand 100))))

(defn init []
  (q/defsketch
    abstract-art
    :host "app"
    :size [800 600]
    :setup setup
    :draw draw
    :middleware [m/fun-mode]))
```

4. setup deps.edn
```clojure
{
 :paths ["src" "resources"]
 :deps  {org.clojure/clojurescript {:mvn/version "1.11.132"}
         quil/quil {:mvn/version "4.3.1563"}}
 :aliases
 {:shadow-cljs {:extra-deps {thheller/shadow-cljs {:mvn/version "2.26.2"}}
                :main-opts ["-m" "shadow.cljs.devtools.cli"]}}}
```

5. setup shadow-cljs.edn
```clojure
{:source-paths ["src"]
 :dependencies [[quil/quil "4.3.1563"]]
 :dev-http {8001 "public"}
 :builds
 {:app
  {:target     :browser
   :output-dir "public/js"
   :asset-path "/js"
   :modules {:main {:entries [abstract-art.core] :init-fn abstract-art.core/init}}}}}
```

6. Add p5.js dependency

```bash
npm install p5
```

This is picked up automatically by shadow-cljs

7. start shawdow-clj

This watches the app build defined in shadow-cljs.edn

```bash
npx shadow-cljs watch app
```

8. Open the index.html

[http://localhost:8001](http://localhost:8001)