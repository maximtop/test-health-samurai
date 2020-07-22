(ns test-health-samurai.validation
  (:require [struct.core :as st]))

(def patient-schema
  [[:id
    st/number]
   [:full_name
    st/required
    st/string]
   [:sex
    st/required]
   [:birthday
    st/required]
   [:address
    st/required]
   [:insurance_number
    st/required
    {:message  "Insurance number should contain 9 digits"
     :validate (fn [num] (= (count num) 9))}]])

(defn validate-patient [params]
  (first (st/validate params patient-schema)))
