(ns yolk-examples.client.macros)

(defn log-sexp [sexp]
  `(yolk.bacon/do-action (yolk-examples.client.debug/log-action '~sexp)))

(defmacro ->logi
  "Skips the fist expression. Useful for using inside a '->"
  [& body]
  `(-> ~(first body)
    ~@(interleave (rest body)
                  (map log-sexp (rest body)))))

(defmacro ->log [& body]
  `(-> ~@(interleave body (map log-sexp body))))