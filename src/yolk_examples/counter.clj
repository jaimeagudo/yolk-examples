(ns yolk-examples.counter
  (:use org.httpkit.server)
  (:require [clojure.edn :as edn])
  (:import rx.Observable
           rx.subscriptions.Subscriptions))

(def cmd (atom []))
(def cnt (atom 0))
(def rate (atom 500))
(def running (atom false))

(defonce count-proc (future
                      (loop []
                        (if @running
                          (swap! cnt inc))
                        (Thread/sleep @rate)
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

(defmethod received :toggle [msg]
  (if (:on? msg)
    (start-counter)
    (stop-counter)))

(defmethod received :rate [msg]
  (reset! rate (:value msg)))

(defn send-to [channel close?]
  (fn [msg]
    (send! channel msg close?)))

(def data (-> cnt
              atom->observable
              (.map (fn [v]
                      (pr-str {:type :message
                               :value (str "Message #" v " from the server.")})))))

(def status (-> running
                atom->observable
                (.map (fn [v]
                        (pr-str {:type :status :value v})))))

(def rate-obs (-> rate
                  atom->observable
                  (.map (fn [v]
                          (pr-str {:type :rate :value v})))))

(def commands (-> cmd
                  atom->observable
                  (.map edn/read-string)
                  (.subscribe received)))

(defn ws [request]
  (with-channel request channel
    (on-receive channel (partial reset! cmd))
    (send! channel
           (pr-str {:type :status :value @running})
           false)
    (.subscribe status (send-to channel false))
    (.subscribe data (send-to channel false))
    (.subscribe rate-obs (send-to channel false))))

(defn poll [request]
  (with-channel request channel
    (.subscribe status (send-to channel true))
    (.subscribe data (send-to channel true))
    (.subscribe rate-obs (send-to channel true))))

(defn cmd-bus [request]
  (prn (-> request :params :message))
  (reset! cmd (-> request :params :message))
  {:status 200})

(defn get-status [request]
  (pr-str {:type :status :value @running}))

(defn initialize []
  (start-counter))
