(ns test-health-samurai.routes.home
  (:require
   [test-health-samurai.layout :as layout]
   [test-health-samurai.db.core :as db]
   [clojure.java.io :as io]
   [test-health-samurai.middleware :as middleware]
   [ring.util.response]
   [ring.util.http-response :as response]))

(defn home-page [request]
  (layout/render
   request
   "home.html"))

(defn patients-list [_]
  (response/ok {:patients (vec (db/get-patients))}))

(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get home-page}]
   ["/patients" {:get patients-list}]
   ["/patient" {:post db/create-patient!}]])

