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
  (js/console.log "received status: " msg)
  (.prop ($ :#toggle) "checked" value)
  (.setupLabel js/window))

(defmethod received :message [{:keys [value] :as msg}]
  (js/console.log "received msg: " msg)
  (j/inner ($ :#msg) value)
  (received {:type :status :value true}))

(defmethod received :default [msg]
  (js/console.log "received default: " msg))

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
    (js/console.log "send-command-ws" (pr-str cmd))
    (.send conn (pr-str cmd))))

(def content
  (template/node
   [:div.container
    [:h1#msg]
    [:label#toggle-label.checkbox {:for "toggle"}
     [:input#toggle {:type "checkbox"}] "Counter Running"]
    [:button#reset.btn.btn-info "Reset Counter"]]))

(defn ^:export main []
  (let [cmd-bus (b/bus)]
    (j/append ($ :#main-content) content)

    (b/plug cmd-bus (-> (ui/->stream ($ :#reset) "click")
                        (b/do-action j/prevent)
                        (b/map {:cmd :reset})
                        (b/merge
                         (-> (ui/->stream ($ :#toggle-label) "click")
                             (b/do-action j/prevent)
                             (b/map #(.is ($ :#toggle) ":checked"))
                             b/not
                             (b/map #(hash-map :cmd :toggle :on? %))))))

    #_(b/on-value cmd-bus (send-command-to "/cmd"))

    (b/on-value cmd-bus (send-command-ws ws-conn))

    #_(-> (net/ajax {:url "/status"})
          (b/merge (lp/long-poll lp-url))
          (b/map read-string)
          (b/on-value received))

    (-> (ws/ws-stream ws-conn)
        (b/map #(read-string (.-data %)))
        (b/on-value #(received %)))))