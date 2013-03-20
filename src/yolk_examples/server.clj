(ns yolk-examples.server
  (:use org.httpkit.server)
  (:require [yolk-examples.pages :as pages]
            [yolk-examples.counter :as counter]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.util.response :as response]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]))

(defroutes app-routes
  (GET "/ws" [] counter/ws)
  (GET "/poll" [] counter/poll)
  (GET "/status" [] counter/get-status)
  (POST "/cmd" [] counter/cmd-bus)
  (GET "/ex/:module/:mode" [module mode] (pages/layout mode module))
  (GET "/" [] (response/redirect "/ex/autocomplete/development"))
  (route/resources "/")
  (route/not-found "Not Found"))

(def app (-> app-routes
             handler/site
             wrap-params
             wrap-keyword-params))

(defn -main [& args]
  (counter/initialize)
  (run-server #'app {:port 3000}))

(comment
  (yolk-examples.server/-main))