(ns mindmap.core
 (:require
  [reagent.core :as r]
  [reagent.dom :as dom]
  [clojure.string :as str]
  [clojure.set :as set]))

;; ----------------------------------------------------------------------------
;; State
;; ----------------------------------------------------------------------------

(defonce app-state
         (r/atom
          {:next-id 2
           :nodes   {1 {:id     1
                        :label  "Root"
                        :x      300
                        :y      50
                        :parent nil}}}))

;; ----------------------------------------------------------------------------
;; Helpers
;; ----------------------------------------------------------------------------

(defn descendants
 "Return a set of all descendant IDs of the node with `id` in the `nodes` map."
 [nodes id]
 (let [direct-children (->> nodes
                            vals
                            (filter #(= id (:parent %)))
                            (map :id)
                            set)]
  (reduce
   (fn [acc child-id]
    (set/union acc (descendants nodes child-id)))
   direct-children
   direct-children)))

(defn remove-node!
 "Given a node-id, confirm with the user and remove that node plus all its descendants."
 [node-id]
 (let [{:keys [nodes]} @app-state
       node (get nodes node-id)
       label (:label node)
       ;; gather all to-be-removed IDs
       child-ids (descendants nodes node-id)
       to-remove (set/union child-ids #{node-id})]
  (when (js/confirm (str "Delete node \"" label "\" and all its children?"))
   (swap! app-state update :nodes
          (fn [old-nodes]
           (apply dissoc old-nodes to-remove))))))

(defn add-node!
 "Given a parent-id, prompt the user for a label and add a new node under that parent.
  Each new child is offset so that they fan out evenly (0, +120, –120, +240, –240, …)."
 [parent-id]
 (let [label (js/prompt "New node label:" "")
       {:keys [nodes next-id]} @app-state]
  (when (and label (not (str/blank? label)))
   (let [parent (get nodes parent-id)
         ;; count how many children parent already has:
         sibling-ids (->> nodes
                          vals
                          (filter #(= parent-id (:parent %)))
                          count)
         ;; k = index for this new child (0-based)
         k sibling-ids
         ;; multiplier = ceil(k/2) as integer division: (quot (inc k) 2)
         multiplier (quot (inc k) 2)
         ;; sign: odd k ⇒ +1, even k ⇒ –1 (so sequence is 0, +, –, +, –, …)
         sign (if (odd? k) 1 -1)
         ;; dx will be 120 * multiplier * sign; when k=0 ⇒ multiplier=0 ⇒ dx=0
         dx (* 120 multiplier sign)
         dy 100
         new-x (+ (:x parent) dx)
         new-y (+ (:y parent) dy)
         new-node {:id     next-id
                   :label  label
                   :x      new-x
                   :y      new-y
                   :parent parent-id}]
    (swap! app-state update :nodes assoc next-id new-node)
    (swap! app-state assoc :next-id (inc next-id))))))

;; ----------------------------------------------------------------------------
;; Rendering
;; ----------------------------------------------------------------------------

(defn draw-edges []
 "Return a sequence of [:line …] hiccup for each parent→child link."
 (let [{:keys [nodes]} @app-state]
  (for [{:keys [id x y parent]} (vals nodes)
        :when parent]
   (let [{px :x py :y} (get nodes parent)]
    [:line {:key          (str "edge-" parent "-" id)
            :x1           px
            :y1           py
            :x2           x
            :y2           y
            :stroke       "#555"
            :stroke-width 2}]))))

(defn draw-nodes []
 "Return a sequence of [:g …] hiccup for each node (circle + text),
  with an on-click that either removes (if shift-click) or adds a child."
 (let [{:keys [nodes]} @app-state]
  (for [{:keys [id x y label]} (vals nodes)]
   [:g {:key       (str "node-" id)
        :transform (str "translate(" x "," y ")")
        :on-click  (fn [e]
                    (.stopPropagation e)
                    (if (.-shiftKey e)
                     (remove-node! id)
                     (add-node! id)))}
    ;; Draw circle
    [:circle {:r            30
              :fill         "#FFD"
              :stroke       "#333"
              :stroke-width 2}]
    ;; Draw label centered
    [:text {:text-anchor "middle"
            :y           5
            :font-size   "12px"
            :fill        "#000"}
     label]])))

(defn mindmap-component []
 [:div {:style {:text-align "center"}}
  [:h2 "Simple ClojureScript Mind-Map"]
  [:p "Click on a node to add a child. Shift+Click on a node to delete it and its descendants."]
  [:svg {:width  800
         :height 600
         :style  {:border "1px solid #CCC"}}
   ;; Draw edges first
   (draw-edges)
   ;; Then draw nodes on top
   (draw-nodes)]])

;; ----------------------------------------------------------------------------
;; Mount
;; ----------------------------------------------------------------------------

(defn ^:export init []
 (dom/render [mindmap-component]
             (.getElementById js/document "app")))

(defn ^:after-load on-reload []
 ;; For hot-reload if using Figwheel/Shadow-CLJS
 )
