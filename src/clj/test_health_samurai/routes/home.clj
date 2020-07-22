(ns test-health-samurai.routes.home
  (:require
   [clojure.instant :as instant]
   [ring.util.http-response :as response]
   [ring.util.response]
   [test-health-samurai.db.core :as db]
   [test-health-samurai.layout :as layout]
   [test-health-samurai.middleware :as middleware]
   [test-health-samurai.validation :refer [validate-patient]]))

(defn home-page [request]
  (layout/render
    request
    "home.html"))

(defn patients-list [_]
  (response/ok {:patients (map
                            (fn [patient] (update-in patient [:birthday] #(.toString %)))
                            (vec (db/get-patients)))}))

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

(defn delete-patient! [{:keys [params]}]
  (println params)
  (try
    (db/delete-patient! params)
    (response/ok {:status :ok})
    (catch Exception e
      (println e)
      (response/internal-server-error
        {:errors {:server-error ["Failed to delete patient!"]}}))))

(defn update-patient! [{:keys [params path-params]}]
  (println params path-params)
  (if-let [errors (validate-patient params)]
    (response/bad-request {:errors errors})
    (try
      (db/edit-patient! (assoc params
                               :birthday (instant/read-instant-date (:birthday params))
                               :id (Integer/parseInt (:id path-params))))
      (response/ok {:status :ok})
      (catch Exception e
        (println e)
        (response/internal-server-error
          {:errors {:server-error ["Failed to update patient!"]}})))))

(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get home-page}]
   ["/patients" {:get    patients-list
                 :delete delete-patient!}]
   ["/patient" {:post new-patient!}]
   ["/patients/:id" {:patch update-patient!
                     :put   update-patient!}]])
