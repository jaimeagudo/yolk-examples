(ns yolk-examples.client.long-poll
  (:require [yolk.bacon :as b]
            [jayq.core :refer [$] :as j]))

(defn poll [url bus]
  (-> (j/ajax url)
      (.done #(do
                (b/push bus %)
                (poll url bus)))))

(defn long-poll [url]
  (let [read-bus (b/bus)]
    (js/setTimeout #(poll url read-bus) 1)
    read-bus))