(ns yolk-examples.client.paint
  (:require [jayq.core :refer [$] :as j]
            [dommy.template :as template]
            [yolk.bacon :as b]
            [yolk.ui :as ui]
            [clojure.browser.repl :as repl]))

(defn mouse-drag [target]
  (b/flat-map (ui/->stream target "mousedown")
              (fn [e]
                (-> (ui/->stream target "mousemove")
                    (b/map (juxt #(.-offsetX %) #(.-offsetY %)))
                    (.slidingWindow 2)
                    (b/map js->clj)
                    (.skip 2)
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
