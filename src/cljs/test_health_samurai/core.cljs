(ns test-health-samurai.core
  (:require [reagent.core :as r]
            [reagent.dom :as rd]
            [ajax.core :refer [GET POST]]
            [clojure.string :as string]))

(defn get-patients [patients]
  (GET "/patients"
       {:headers {"Accept" "application/transit+json"}
        :handler #(reset! patients (:patients %))}))

(defn log [& args] 
  (apply (.-log js/console) args))

(defn patients-list [patients]
  (log patients)
  [:div
   [:table.table.is_fullwidth
    [:thead
     [:tr
      [:th "Name"]
      [:th "Sex"]
      [:th "Birthday"]
      [:th "Address"]
      [:th "Insurance number"]]]
    [:tbody
     (for [{:keys [id, full_name, sex, birthday, address, insurance_number]} @patients]
       [:tr {:key id}
        [:td full_name]
        [:td sex]
        [:td "birthday"]
        [:td address]
        [:td insurance_number]])
     ]]])

(defn add-patient! [fields errors]
  (println @fields)
  (POST "/patient"
        {:format :json
         :headers
         {"Accept" "application/transit+json"
          "x-csrf-token" (. js/window -csrfToken)}
         :params @fields
         :handler #(do
                     (.log js/console (str "response:" %))
                     (reset! errors nil))
         :error-handler #(do
                           (.log js/console (str %))
                           (reset! errors (get-in % [:response :errors])))}))

(defn errors-component [errors id]
  (when-let [error (id @errors)]
    [:div.notification.is-danger (string/join error)]))

(defn patients-form []
  (let [fields (r/atom {})
        errors (r/atom nil)]
    (fn []
      [:div
       [errors-component errors :server-error]
       [:div.field
        [:label.label {:for :full_name} "Full name"]
        [errors-component errors :full_name]
        [:input.input
         {:type :text
          :name :full_name
          :on-change #(swap! fields assoc :full_name (-> % .-target .-value))
          :value (:full_name @fields)}]]
       [:div.field
        [:label.label {:for :sex} "Sex"]
        [errors-component errors :sex]
        [:input.input
         {:name :sex
          :value (:sex @fields)
          :on-change #(swap! fields assoc :sex (-> % .-target .-value))}]]
       [:div.field
        [:label.label {:for :birthday} "Birthday"]
        [errors-component errors :birthday]
        [:input.input
         {:type :date
          :name :birthday
          :on-change #(swap! fields assoc :birthday (-> % .-target .-value))
          :value (:birthday @fields)}]]
       [:div.field
        [:label.label {:for :address} "Address"]
        [errors-component errors :address]
        [:input.input
         {:type :text
          :name :address
          :on-change #(swap! fields assoc :address (-> % .-target .-value))
          :value (:address @fields)}]]
       [:div.field
        [:label.label {:for :insurance_number} "Insurance number"]
        [errors-component errors :insurance_number]
        [:input.input
         {:type :text
          :name :insurance_number
          :on-change #(swap! fields assoc :insurance_number (-> % .-target .-value))
          :value (:insurance_number @fields)}]]
       [:input.button.is-primary
        {:type :submit
         :on-click #(add-patient! fields errors)
         :value "Add"}]])))

(defonce patients (r/atom nil))
(get-patients patients)

(defn home []
  (do
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
