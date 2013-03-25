(ns yolk-examples.client.debug)

(defn log-action [sexp]
  (fn [v]
    (js/console.log (str sexp " => ") (pr-str v))))
