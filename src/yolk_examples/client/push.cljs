(ns yolk-examples.client.push
  (:require [jayq.core :refer [$] :as j]
            [dommy.template :as template]
            [yolk.bacon :as b]
            [yolk.ui :as ui]
            [yolk.net :as net]
            [clojure.browser.repl :as repl]))

(def conn (js/WebSocket. "ws://127.0.0.1:3000/ws"))

(defn on-open [conn f]
  (set! (.-onopen conn) f))

(defn on-msg [conn f]
  (set! (.-onmessage conn) f))

(defn on-error [conn f]
  (set! (.-onerror conn) f))

(defn on-close [conn f]
  (set! (.-onclose conn) f))

(defn ws-stream [conn]
  (js/Bacon.EventStream.
   (fn [subscriber]
     (on-open conn (comp subscriber b/initial))
     (on-msg conn (comp subscriber b/next))
     (on-error conn (comp subscriber b/error))
     (on-close conn (comp subscriber b/end))
     (fn []))))

(defn ^:export main []
  (j/append ($ :#main-content)
            (template/node
             [:div.container
              [:h1#msg]
              [:div.toggle
               [:label.toggle-radio {:for "toggle-counter-on"} "ON"]
               [:input#toggle-counter-on {:type "radio"
                                       :value "toggle-on"
                                       :name "toggle-counter"
                                       :checked "checked"}]
               [:label.toggle-radio {:for "toggle-counter-off"} "OFF"]
               [:input#toggle-counter-off {:type "radio"
                                       :value "toggle-off"
                                       :name "toggle-counter"}]]
              [:button#reset.btn.btn-primary "Reset Counter"]]))
  (-> (ui/->stream ($ :#reset) "click")
      (b/do-action j/prevent)
      (b/on-value #(.send conn "")))
  (-> conn ws-stream (b/on-value #(j/inner ($ :#msg) (.-data %)))))