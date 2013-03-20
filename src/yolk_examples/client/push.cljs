(ns yolk-examples.client.push
  (:require [yolk-examples.client.ws :as ws]
            [jayq.core :refer [$] :as j]
            [dommy.template :as template]
            [yolk.bacon :as b]
            [yolk.ui :as ui]
            [yolk.net :as net]
            [clojure.browser.repl :as repl]
            [cljs.reader :as reader]))

(def conn (js/WebSocket. "ws://127.0.0.1:3000/ws"))

(defmulti received :type)

(defmethod received :noop [])

(defmethod received :message [{:keys [value]}]
  (j/inner ($ :#msg) value))

(defmethod received :status [{:keys [value]}]
  (.prop ($ :#toggle) "checked" value)
  (.setupLabel js/window))

(defn read-string [s]
  (try
    (reader/read-string s)
    (catch js/Error e
      (js/console.log e)
      {:type :noop})))

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
      (b/on-value #(.send conn (pr-str {:cmd :reset}))))

  (-> (ui/->stream ($ :#toggle-label) "click")
      (b/map #(.is ($ :#toggle) ":checked"))
      (b/on-value #(do
                     (.send conn (pr-str {:cmd :toggle
                                          :on? %})))))
  (-> (ws/ws-stream conn)
      (b/on-value (fn [resp]
                    (-> (.-data resp)
                        read-string
                        received)))))