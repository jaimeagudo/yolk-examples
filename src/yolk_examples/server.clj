(ns yolk-examples.server
  (:use org.httpkit.server)
  (:require [yolk-examples.pages :as pages]
            [yolk-examples.counter :as counter]
            [compojure.core :refer [defroutes GET]]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.util.response :as response]))

(defroutes app-routes
  (GET "/ws" [] counter/ws)
  (GET "/poll" [] counter/poll)
  (GET "/status" [] counter/get-status)
  (GET "/ex/:module/:mode" [module mode] (pages/layout mode module))
  (GET "/" [] (response/redirect "/ex/autocomplete/development"))
  (route/resources "/")
  (route/not-found "Not Found"))

(def app (handler/site app-routes))

(defn -main [& args]
  (counter/initialize)
  (run-server #'app {:port 3000}))

(comment
  (yolk-examples.server/-main))