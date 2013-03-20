(ns yolk-examples.counter
  (:use org.httpkit.server)
  (:require [clojure.edn :as edn])
  (:import rx.Observable
           rx.subscriptions.Subscriptions))

(def cmd (atom []))
(def cnt (atom 0))
(def running (atom false))

(defonce count-proc (future
                      (loop []
                        (if @running
                          (swap! cnt inc))
                        (Thread/sleep 1000)
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

(defmulti received :cmd)

(defmethod received :reset [msg]
  (reset! cnt 0))

(defmethod received :toggle [{:keys [on?]}]
  (if on?
    (start-counter)
    (stop-counter)))

(defn send-to [channel]
  (fn [msg]
    (send! channel msg false)))

(def data (-> cnt
              atom->observable
              (.map (fn [v]
                      (pr-str {:type :message
                               :value (str "Message #" v " from the server.")})))))

(def status (-> running
                atom->observable
                (.map (fn [v]
                        (pr-str {:type :status :value v})))))

(def commands (-> cmd
                  atom->observable
                  (.map edn/read-string)
                  (.subscribe received)))
(defn counter []
  (start-counter)
  (fn [request]
    (with-channel request channel
      (on-receive channel (partial reset! cmd))
      (send! channel
             (pr-str {:type :status :value @running})
             false)
      (.subscribe status (send-to channel))
      (.subscribe data (send-to channel)))))
