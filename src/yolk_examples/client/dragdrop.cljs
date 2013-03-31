(ns yolk-examples.client.dragdrop
  (:require [jayq.core :refer [$] :as j]
            [dommy.template :as template]
            [yolk.bacon :as b]
            [yolk.ui :as ui]
            [clojure.browser.repl :as repl])
  (:use-macros [yolk.macros :only [->log ->logi]]))

(defn- calc-offset [$target]
  (fn [event]
    (j/prevent event)
    {:left (- (.-clientX event)
              (-> $target j/offset :left))
     :top (- (.-clientY event)
             (-> $target j/offset :top))}))

(defn track-position [move up]
  (fn [{:keys [top left]}]
    (-> move
        (b/map (fn [pos]
                     {:left (- (.-clientX pos) left)
                      :top (- (.-clientY pos) top)}))
        (b/take-until up))))

(defn mouse-drag [$target]
  (let [mouseup (ui/->stream ($ js/document) "mouseup")
        mousemove (ui/->stream ($ js/document) "mousemove")]
    (->log (ui/->stream $target "mousedown")
           (b/map (calc-offset $target))
           (b/flat-map (track-position mousemove mouseup)))))

(def drag-target
  (template/node
   [:div#drag-target {:style {:background-color "#000"
                              :border "1px solid #666"
                              :color "#fff"
                              :padding "10px"
                              :position "absolute"
                              :cursor "move"}}
    "Drag Me!"]))

(defn ^:export main []
  (let [$target ($ drag-target)]
    (j/append ($ "#main-content") $target)
    (b/on-value (mouse-drag $target) (partial j/css $target))))
