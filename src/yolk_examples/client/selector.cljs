(ns yolk-examples.client.selector
  (:require [jayq.core :refer [$] :as j]
            [dommy.template :as template]
            [yolk.bacon :as b]
            [yolk.ui :as ui]
            [yolk.net :as net]
            [clojure.browser.repl :as repl]))

(defn make-model [name price & items]
  {:name name
   :type "model"
   :items items
   :price price})

(defn make-engine [name price & items]
  {:name name
   :type "engine"
   :items items
   :price price})

(def data
  {:models [(make-model "model1" 1000 "engine2" "engine3")
            (make-model "model2" 2000 "engine1" "engine4" "engine5")
            (make-model "model3" 3000 "engine6" "engine9")
            (make-model "model4" 4000 "engine7" "engine8" "engine10")
            (make-model "model5" 5000 "engine1" "engine5" "engine7" "engine9")]
   :engines [(make-engine "engine1" 500 "model2" "model5")
             (make-engine "engine2" 750 "model1")
             (make-engine "engine3" 1000 "model1")
             (make-engine "engine4" 1250 "model2")
             (make-engine "engine5" 1500 "model2" "model5")
             (make-engine "engine6" 1750 "model3")
             (make-engine "engine7" 2000 "model4" "model5")
             (make-engine "engine8" 2250 "model4")
             (make-engine "engine9" 2500 "model3" "model5")
             (make-engine "engine10" 2750 "model4")]})

(defn radio-button [name value label]
  [:label.radio
   [:input {:type "radio" :name name :value value}]
   " " label])

(def content
  (template/node
   [:div.container
    [:h3 "Engine Selector"]
    [:form
     [:fieldset
      [:legend "Choose a model"]
      (for [{:keys [name]} (:models data)]
        (radio-button "model" name name))]
     [:fieldset
      [:legend "Choose an engine"]
      (for [{:keys [name]} (:engines data)]
        (radio-button "engine" name name))]
     [:button#reset.btn.btn-mini "Reset"]]
    [:p#total-price "$ 000"]]))

(defn ^:export main []
  (j/append ($ "#main-content") content))