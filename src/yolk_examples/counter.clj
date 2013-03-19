(ns yolk-examples.counter
  (:use org.httpkit.server)
  (:import rx.Observable
           rx.subscriptions.Subscriptions))

(def cnt (atom 0))

(def count-proc (atom nil))

(defn start-counter []
  (if-not @count-proc
    (reset! count-proc
            (future
              (loop []
                (swap! cnt inc)
                (Thread/sleep (* (+ (rand-int 7) 3) 100))
                (recur))))))

(defn stop-counter []
  (when @count-proc
    (future-cancel @count-proc)
    (reset! count-proc nil)))

(def data
  (-> (Observable/create
       (fn [observer]
         (let [k (gensym)]
           (add-watch cnt k
                      (fn [_ _ _ new]
                        (.onNext observer new)))
           (Subscriptions/create #(remove-watch cnt k)))))))

(defn counter []
  (start-counter)
  (fn [request]
    (with-channel request channel
      (on-receive channel (fn [e] (reset! cnt 0)))
      (-> data
          (.subscribe (fn [i]
                        (send! channel
                               (str "Message #" i " from the server.")
                               false)))))))
