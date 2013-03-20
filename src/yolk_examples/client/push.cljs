(ns yolk-examples.client.push
  (:require [yolk-examples.client.ws :as ws]
            [yolk-examples.client.long-poll :as lp]
            [jayq.core :refer [$] :as j]
            [dommy.template :as template]
            [yolk.bacon :as b]
            [yolk.ui :as ui]
            [yolk.net :as net]
            [clojure.browser.repl :as repl]
            [cljs.reader :as reader]))

(def ws-conn (js/WebSocket. "ws://localhost:3000/ws"))
(def lp-url "http://localhost:3000/poll")

(defmulti received :type)

(defmethod received :status [{:keys [value]}]
  (.prop ($ :#toggle) "checked" value)
  (.setupLabel js/window))

(defmethod received :message [{:keys [value] :as msg}]
  (j/inner ($ :#msg) value)
  (received {:type :status :value true}))

(defmethod received :default [msg])

(defn read-string [s]
  (try
    (reader/read-string s)
    (catch js/Error e
      (js/console.log e))))

(defn send-command-to [url]
  (fn [cmd]
    (-> (net/ajax {:url url
                   :type "POST"
                   :data {:message (pr-str cmd)}})
        (b/on-value identity))))

(defn send-command-ws [conn]
  (fn [cmd]
    (.send conn (pr-str cmd))))

(def content
  (template/node
   [:div.container
    [:h1#msg]
    [:label#toggle-label.checkbox {:for "toggle"}
     [:input#toggle {:type "checkbox"}] "Counter Running"]
    [:button#reset.btn.btn-info "Reset Counter"]]))

(defn lp-message-stream []
  (-> (net/ajax {:url "/status"})
          (b/merge (lp/long-poll lp-url))
          (b/map read-string)))

(defn ws-message-stream []
  (-> ws-conn
      ws/ws-stream
      (b/map #(.-data %))
      (b/map read-string)))

(defn message-stream []
  (if js/WebSocket
    (ws-message-stream)
    (lp-message-stream)))

(defn command-handler []
  (if js/WebSocket
    (send-command-ws ws-conn)
    (send-command-to "/cmd")))

(defn cmd-bus []
  (let [bus (b/bus)
        reset-stream (-> (ui/->stream ($ :#reset) "click")
                         (b/do-action j/prevent)
                         (b/map {:cmd :reset}))
        toggle-stream (-> (ui/->stream ($ :#toggle-label) "click")
                          (b/do-action j/prevent)
                          (b/map #(.is ($ :#toggle) ":checked"))
                          b/not
                          (b/map #(hash-map :cmd :toggle :on? %)))]
    (b/plug bus
            (b/merge reset-stream toggle-stream))
    bus))

(defn ^:export main []
  (j/append ($ :#main-content) content)
  (b/on-value (cmd-bus) (command-handler))
  (b/on-value (message-stream) #(received %)))