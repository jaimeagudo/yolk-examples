(ns yolk-examples.client.push
  (:require [yolk-examples.client.ws :as ws]
            [jayq.core :refer [$] :as j]
            [dommy.template :as template]
            [yolk.bacon :as b]
            [yolk.ui :as ui]
            [yolk.net :as net]
            [clojure.browser.repl :as repl]))

(def host (-> js/window .-location .-host))
(def ws-url (str "ws://" host "/ws"))
(def poll-url "/poll")
(def init-url "/status")
(def cmd-url "/cmd")

(defmulti received :type)

(defmethod received :status [{:keys [value]}]
  (.prop ($ :#toggle) "checked" value)
  (.setupLabel js/window))

(defmethod received :message [{:keys [value] :as msg}]
  (j/inner ($ :#msg) value)
  (received {:type :status :value true}))

(defmethod received :default [msg])

(def content
  (template/node
   [:div.container
    [:h1#msg]
    [:label#toggle-label.checkbox {:for "toggle"}
     [:input#toggle {:type "checkbox"}] "Counter Running"]
    [:button#reset.btn.btn-info "Reset Counter"]]))

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
  (let [[message-stream command-handler] (ws/connect ws-url
                                                     init-url
                                                     poll-url
                                                     command-url)]
    (b/on-value (cmd-bus) command-handler)
    (b/on-value message-stream #(received %))))