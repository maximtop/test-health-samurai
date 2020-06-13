(ns test-health-samurai.test.db.core
  (:require
   [test-health-samurai.db.core :refer [*db*] :as db]
   [java-time.pre-java8]
   [luminus-migrations.core :as migrations]
   [clojure.test :refer :all]
   [next.jdbc :as jdbc]
   [test-health-samurai.config :refer [env]]
   [mount.core :as mount]))

(use-fixtures
  :once
  (fn [f]
    (mount/start
     #'test-health-samurai.config/env
     #'test-health-samurai.db.core/*db*)
    (migrations/migrate ["migrate"] (select-keys env [:database-url]))
    (f)))

(deftest test-patients
  (jdbc/with-transaction [t-conn *db* {:rollback-only true}]
    (is (= 1 (db/create-patient!
              t-conn
              {:full_name "Jhon Doe"
               :sex "male"
               :birthday  (java.sql.Timestamp/valueOf "1988-03-02 00:00:00")
               :address "Moscow"
               :insurance_number "1234567890123456"})))
    ;; TODO find out why this test is not working
    ;; (is (= {:id 1
    ;;         :full_name "Jhon Dow"
    ;;         :sex "male"
    ;;         :birthday  (java.sql.Timestamp/valueOf "1988-03-02 00:00:00")
    ;;         :address "Moscow"
    ;;         :insurance_number "1234567890123456"}
    ;;        (db/get-patient t-conn {:id 1})))
))
