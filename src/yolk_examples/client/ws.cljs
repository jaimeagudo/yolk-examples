(ns yolk-examples.client.ws
  (:require [yolk.bacon :as b]))

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
