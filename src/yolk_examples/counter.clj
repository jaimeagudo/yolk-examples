(ns yolk-examples.counter
  (:use org.httpkit.server)
  (:import rx.Observable
           rx.subscriptions.Subscriptions))

(def cnt (atom 0))
(def running (atom false))

(defonce count-proc (future
                      (loop []
                        (if @running
                          (swap! cnt inc))
                        (Thread/sleep 750)
                        (recur))))

(defn start-counter []
  (reset! running true))

(defn stop-counter []
  (reset! running false))

(defn atom->observable [atm]
  (Observable/create
       (fn [observer]
         (let [k (gensym)]
           (add-watch atm k
                      (fn [_ _ _ new]
                        (.onNext observer new)))
           (Subscriptions/create #(remove-watch atm k))))))

(def data (atom->observable cnt))
(def status (atom->observable running))

(defmulti received :cmd)

(defmethod received :reset [msg]
  (reset! cnt 0))

(defmethod received :toggle [{:keys [on?]}]
  (if on?
    (start-counter)
    (stop-counter)))

(defn counter []
  (start-counter)
  (fn [request]
    (with-channel request channel
      (on-receive channel (fn [e]
                            (received (read-string e))))
      (send! channel
             (pr-str {:type :status :value @running})
             false)
      (-> status
          (.subscribe (fn [b]
                        (send! channel
                               (pr-str {:type :status :value b})
                               false))))
      (-> data
          (.subscribe (fn [i]
                        (send! channel
                               (pr-str
                                {:type :message
                                 :value (str "Message #" i " from the server.")})
                               false)))))))
