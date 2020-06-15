(ns test-health-samurai.routes.home
  (:require
   [test-health-samurai.layout :as layout]
   [test-health-samurai.db.core :as db]
   [clojure.java.io :as io]
   [test-health-samurai.middleware :as middleware]
   [ring.util.response]
   [ring.util.http-response :as response]
   [struct.core :as st]
   [clojure.instant :as instant]))

(defn home-page [request]
  (layout/render
   request
   "home.html"))

(defn patients-list [_]
  (response/ok {:patients (vec (db/get-patients))}))

;; TODO validate better
(def patient-schema
  [[:full_name
    st/required
    st/string]
   [:sex
    st/required]
   [:birthday
    st/required]
   [:address
    st/required]
   [:insurance_number
    st/required]])

(defn validate-patient [params]
  (first (st/validate params patient-schema)))

(defn new-patient! [{:keys [params]}]
  (println params)
  (if-let [errors (validate-patient params)]
    (response/bad-request {:errors errors})
    (try
      (db/new-patient!
       (assoc params :birthday (instant/read-instant-date (:birthday params))))
      (response/ok {:status :ok})
      (catch Exception e
        (println e)
        (response/internal-server-error
         {:errors {:server-error ["Failed to save message!"]}})))))


;; instant/read-instant-date

(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get home-page}]
   ["/patients" {:get patients-list}]
   ["/patient" {:post new-patient!}]])

