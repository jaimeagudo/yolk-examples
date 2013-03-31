(ns yolk-examples.client.paint
  (:require [jayq.core :refer [$] :as j]
            [dommy.template :as template]
            [yolk.bacon :as b]
            [yolk.ui :as ui]
            [clojure.browser.repl :as repl])
  (:use-macros [yolk.macros :only [->log ->logi]]))

(defn mouse-drag [target]
  (b/flat-map (ui/->stream target "mousedown")
              (fn [e]
                (->log (ui/->stream target "mousemove")
                       (b/throttle 300)
                       (b/map (juxt #(.-offsetX %) #(.-offsetY %)))
                       (b/sliding-window 2)
                       (b/map js->clj)
                       (b/skip 2)
                       (b/take-until (ui/->stream ($ js/document) "mouseup"))))))


(defn ^:export main []
  (j/append ($ "#main-content")
            "<canvas id=\"canvas\" height=\"768\" width=\"1024\"/>")
  (let [$canvas ($ :#canvas)
        ctx (.getContext (first $canvas) "2d")]
    (j/css $canvas "border" "5px solid #999")
    (b/on-value (mouse-drag $canvas)
                (fn [[[x1 y1] [x2 y2]]]
                  (.moveTo ctx x1 y1)
                  (.lineTo ctx x2 y2)
                  (.stroke ctx)))))
