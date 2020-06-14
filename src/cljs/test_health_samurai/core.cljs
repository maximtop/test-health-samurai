(ns test-health-samurai.core
  (:require [reagent.core :as r]
            [reagent.dom :as rd]))

(defn patient-form []
  (let [fields (r/atom {})]
    (fn []
      [:div
       [:div.field
        [:label.label {:for :name} "Full name"]
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
  [:div.content>div.columns.is-centered>div.column.is-two-thirds
   [:div.columns>div.column
    [patient-form]]])

(defn mount-components [] 
  (rd/render [home]  (.getElementById js/document "content")))

(defn init! []
  (mount-components))
