(ns yolk-examples.client.fly
  (:require [jayq.core :refer [$] :as j]
            [dommy.template :as template]
            [yolk.bacon :as b]
            [yolk.ui :as ui]
            [clojure.browser.repl :as repl]))

(def mm (ui/mousemove ($ js/document)))

(defn letter-span [letter]
  (template/node
   [:span {:style {:position "absolute"
                   :font-family "\"Lucida Console\", Monaco, monospace"}}
    letter]))

(defn bind-letter [$content letter i]
  (let [s ($ (letter-span letter))
        stream (b/delay mm (* i 50))]
    (j/append $content s)
    (-> stream
        (b/map #(+ (.-clientX %) (* i 10) 15))
        (b/on-value #(j/css s {:left (str % "px")})))
    (-> stream
        (b/map #(.-clientY %))
        (b/on-value #(j/css s {:top (str % "px")})))))

(defn fly-text [text]
  (let [$content ($ :#main-content)]
    (j/empty $content)
    (doseq [i (range 0 (count text))]
      (bind-letter $content (get text i) i))))

(defn ^:export main []
  (let [text "Time flies like an arrow"]
    (fly-text text)))

(fly-text "Fruit flies like a orange.")

(defn ^:export repl []
  (repl/connect "http://localhost:9000/repl"))
