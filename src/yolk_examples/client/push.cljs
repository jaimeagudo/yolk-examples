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

(def wsconn (js/WebSocket. "ws://localhost:3000/ws"))
(def lp-url "http://localhost:3000/poll")

(defmulti received :type)

(defmethod received :default [])

(defmethod received :status [{:keys [value]}]
  (.prop ($ :#toggle) "checked" value)
  (.setupLabel js/window))

(defmethod received :message [{:keys [value]}]
  (j/inner ($ :#msg) value)
  (received {:type :status :value true}))

(defn read-string [s]
  (try
    (reader/read-string s)
    (catch js/Error e
      (js/console.log e))))

(defn ^:export main []
  (j/append ($ :#main-content)
            (template/node
             [:div.container
              [:h1#msg]
              [:label#toggle-label.checkbox {:for "toggle"}
               [:input#toggle {:type "checkbox"}] "Counter Running"]
              [:button#reset.btn.btn-info "Reset Counter"]]))

  (-> (ui/->stream ($ :#reset) "click")
      (b/do-action j/prevent)
      (b/on-value #(.send wsconn (pr-str {:cmd :reset}))))

  (-> (ui/->stream ($ :#toggle-label) "click")
      (b/map #(.is ($ :#toggle) ":checked"))
      (b/on-value #(do
                     (.send wsconn (pr-str {:cmd :toggle
                                            :on? %})))))
  (-> (net/ajax {:url "/status"})
      (b/merge (lp/long-poll lp-url))
      (b/on-value (fn [resp]
                    (js/console.log resp)
                    (-> resp
                        read-string
                        received)))))