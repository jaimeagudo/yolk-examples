(ns yolk-examples.client.dining
  (:require [jayq.core :refer [$] :as j]
            [dommy.template :as template]
            [yolk.bacon :as b]
            [yolk.ui :as ui]
            [yolk.net :as net]
            [clojure.browser.repl :as repl])
  (:use-macros [yolk.macros :only [->log ->logi]]))


(def $main ($ :#main-content))

(defn log-div [t]
  (j/append $main
            (template/node [:h3 t])))

(defn ^:export main []
  (let [[c1 c2 c3 :as chopsticks] [(b/bus) (b/bus) (b/bus)]
        [h1 h2 h3 :as hungry] [(b/bus) (b/bus) (b/bus)]
        eat (fn [i]
              (fn []
                (js/setTimeout (fn []
                                 (log-div "Done!")
                                 (b/push (nth chopsticks i) {})
                                 (b/push (nth chopsticks (-> i inc (mod 3))) {}))
                               1000)
                (str "Philosopher " (inc i) " eating.")))
        
        dining (b/when [h1 c1 c2] (eat 0)
                       [h2 c2 c3] (eat 1)
                       [h3 c3 c1] (eat 2))]

    (-> dining (b/on-value log-div))

    (doseq [c chopsticks] (b/push c {}))
    (doseq [h hungry] (b/push h {}))))
