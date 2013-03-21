(ns yolk-examples.client.push
  (:require [yolk-examples.client.ws :as ws]
            [jayq.core :refer [$] :as j]
            [dommy.template :as template]
            [yolk.bacon :as b]
            [yolk.ui :as ui]
            [yolk.net :as net]
            [clojure.browser.repl :as repl]))

(def host (-> js/window .-location .-host))

(defmulti received :type)

(defmethod received :status [{:keys [value]}]
  (.prop ($ :#toggle) "checked" value)
  (.setupLabel js/window))

(defmethod received :message [{:keys [value] :as msg}]
  (j/inner ($ :#msg) value)
  (received {:type :status :value true}))

(defmethod received :rate [msg]
  (when (not= (:value msg) (.slider ($ :#slider) "value"))
    (.slider ($ :#slider) "value" (:value msg))
    (j/inner ($ :#rate) (str (:value msg) "ms") )))

(defmethod received :default [msg])

(def content (template/node
              [:div.container
               [:h1#msg]
               [:h3#rate]
               [:div#slider.ui-slider]
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

(defn ^:export main []
  (j/append ($ :#main-content) content)

  (.slider ($ :#slider)
           (clj->js {:min 100 :max 1000 :value 500
                     :orientation "horizontal" :range "min"}))

  (-> (ui/->stream ($ :#slider) "slide")
      (b/map #(.slider ($ :#slider) "value"))
      (b/on-value (fn [v]
                    (j/inner ($ :#rate) (str v "ms") ))))



  (let [[messages handler] (ws/connect (str "ws://" host "/ws")
                                       "/status" "/poll" "/cmd")
        cmd-bus (b/bus)
        change-rate (-> (ui/->stream ($ :#slider) "slidechange")
                        (b/map #(.slider ($ :#slider) "value"))
                        (b/map (fn [v] {:cmd :rate :value v})))]
    (b/plug cmd-bus (b/merge-all [(click-command ($ :#reset) reset-command)
                                  (click-command ($ :#toggle-label) toggle-command)
                                  change-rate]))
    (b/log cmd-bus)
    (b/on-value cmd-bus handler)
    (b/on-value messages #(received %))))