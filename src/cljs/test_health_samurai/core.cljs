(ns test-health-samurai.core
  (:require [reagent.core :as r]
            [reagent.dom :as rd]
            [ajax.core :refer [GET POST]]))

(defn get-patients [patients]
  (GET "/patients"
       {:headers {"Accept" "application/transit+json"}
        :handler #(reset! patients (:patients %))}))

(defn patients-list [patients]
  (println patients)
  [:ul.patients
   (for [{:keys [full_name]} @patients]
     ^{:key full_name}
     [:li
      [:p full_name]])])

(defn patients-form []
  (let [fields (r/atom {})]
    (fn []
      [:div
       [:div.field
        [:label.label {:for :full_name} "Full name"]
        [:input.input
         {:type :text
          :name :full_name
          :on-change #(swap! fields assoc :full_name (-> % .-target .-value))
          :value (:full_name @fields)}]]
       [:div.field
        [:label.label {:for :sex} "Sex"]
        [:input.input
         {:name :sex
          :value (:sex @fields)
          :on-change #(swap! fields assoc :sex (-> % .-target .-value))}]]
       [:div.field
        [:label.label {:for :birthday} "Birthday"]
        [:input.input
         {:type :date
          :name :birthday
          :on-change #(swap! fields assoc :birthday (-> % .-target .-value))
          :value (:birthday @fields)}]]
       [:div.field
        [:label.label {:for :address} "Address"]
        [:input.input
         {:type :text
          :name :address
          :on-change #(swap! fields assoc :address (-> % .-target .-value))
          :value (:address @fields)}]]
       [:div.field
        [:label.label {:for :insurance_number} "Insurance number"]
        [:input.input
         {:type :text
          :name :insurance_number
          :on-change #(swap! fields assoc :insurance_number (-> % .-target .-value))
          :value (:insurance_number @fields)}]]
       [:input.button.is-primary
        {:type :submit
         :value "Add"}]])))

(defn home []
  (let [patients (r/atom nil)]
    (get-patients patients)
    (fn []
      [:div.content>div.columns.is-centered>div.column.is-two-thirds
       [:div.columns>div.column
        [:h3 "Patients list"]
        [patients-list patients]]
       [:div.columns>div.column
        [patients-form]]])))
       

(defn mount-components [] 
  (rd/render [home]  (.getElementById js/document "content")))

(defn init! []
  (mount-components))
