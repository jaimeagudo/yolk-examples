(ns yolk-examples.client.ws
  (:require [yolk.bacon :as b]
            [yolk.net :as net]
            [jayq.core :refer [$] :as j]
            [cljs.reader :as reader]))

(defn read-string [s]
  (try
    (reader/read-string s)
    (catch js/Error e
      (js/console.log e))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; WebSocket
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


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

(defn ws-message-stream [ws-conn]
  (-> ws-conn
      ws-stream
      (b/map #(.-data %))
      (b/map read-string)))


(defn ws-send-command [conn]
  (fn [cmd]
    (.send conn (pr-str cmd))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Long Polling
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn poll [url bus]
  (-> (j/ajax url)
      (.always (fn [e]
                 (b/push bus e)
                 (js/setTimeout #(poll url bus) 10)))))

(defn long-poll [url]
  (let [read-bus (b/bus)]
    (js/setTimeout #(poll url read-bus) 1)
    read-bus))

(defn lp-message-stream [init-url poll-url]
  (-> (net/ajax {:url init-url})
      (b/merge (long-poll poll-url))
      (b/map read-string)))

(defn lp-send-command [url]
  (fn [cmd]
    (-> (net/ajax {:url url
                   :type "POST"
                   :data {:message (pr-str cmd)}})
        (b/on-value identity))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Unified Interface
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn connect [ws-url init-url poll-url command-url]
  (let [ws-conn (if js/window.WebSocket (js/WebSocket. ws-url))]
    (if ws-conn
      [(ws-message-stream ws-conn)
       (ws-send-command ws-conn)]
      [(lp-message-stream init-url poll-url)
       (lp-send-command command-url)])))