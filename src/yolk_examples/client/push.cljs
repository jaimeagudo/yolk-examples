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

(defn toggle-command [_]
  {:cmd :toggle
   :on?  (not (.is ($ :#toggle) ":checked"))})

(defn reset-command [_]
  {:cmd :reset})

(defn click-command [$elem cmd-fn]
  (-> $elem ui/click (b/map cmd-fn)))

(defn cmd-stream []
  (b/merge (click-command ($ :#reset) reset-command)
           (click-command ($ :#toggle-label) toggle-command)))

(defn ^:export main []
  (j/append ($ :#main-content) content)
  (let [[message-stream command-handler] (ws/connect ws-url
                                                     init-url
                                                     poll-url
                                                     command-url)
        cmd-bus (b/bus)]
    (b/plug cmd-bus (cmd-stream))
    (b/on-value cmd-bus command-handler)
    (b/on-value message-stream #(received %))))