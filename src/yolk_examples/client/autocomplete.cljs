(ns yolk-examples.client.autocomplete
  (:require [jayq.core :refer [$] :as j]
            [dommy.template :as template]
            [yolk.bacon :as b]
            [yolk.ui :as ui]
            [yolk.net :as net]
            [clojure.browser.repl :as repl])
  (:use-macros [yolk.macros :only [->log ->logi]]))

(defn throttled-input []
  (-> (ui/->stream ($ "#search-input") "keyup")
      (b/map #(j/val ($ "#search-input")))
      (b/throttle 500)
      (b/filter #(> (count %) 2))
      b/skip-duplicates))

(defn search-wikipedia [term]
  (net/ajax {:url "http://en.wikipedia.org/w/api.php"
             :data {:action "opensearch"
                    :search term
                    :format :json}
             :dataType :jsonp}))

(defn suggestions []
  (-> (throttled-input)
      (b/flat-map-latest search-wikipedia)
      (b/map #(js->clj % :keywordize-keys true))
      (b/filter (fn [data]
                  (and (:0 data) (:1 data))))))

(defn display-response  [$elem]
  (fn [data]
    (j/empty $elem)
    (doseq [t (:1 data)]
      (j/append $elem (template/node [:li t])))))

(def content (template/node
              [:div.row
               [:h2 "Search Wikipedia"]
               [:input#search-input.input-xlarge]
               [:ul#results.unstyled]]))

(defn ^:export main []
  (j/append ($ "#main-content") content)
  (let [$elem ($ "#results")]
    (b/on-value (suggestions)
                (display-response $elem)))
  (.focus ($ :#search-input)))
