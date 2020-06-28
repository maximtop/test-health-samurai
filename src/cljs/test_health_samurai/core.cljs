(ns test-health-samurai.core
  (:require [reagent.core :as r]
            [reagent.dom :as rd]
            [ajax.core :refer [GET POST DELETE]]
            [clojure.string :as string]))

(defn get-patients [patients]
  (GET "/patients"
       {:headers {"Accept" "application/transit+json"}
        :handler #(reset! patients (:patients %))}))

(defn log [& args]
  (apply (.-log js/console) args))

(defn delete-patient [id]
  (fn [patients] (DELETE "/patients"
                         {:format        :json
                          :headers
                          {"Accept"       "application/transit+json"
                           "x-csrf-token" (. js/window -csrfToken)}
                          :params        {:id id}
                          :handler       #(do
                                            (log %)
                                            (swap! patients (partial remove (fn [patient](= (:id patient) id)))))
                          :error-handler #(do
                                            (log %))}))) ;; TODO add toast notification

;; TODO do not display if patients list is empty
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
      [:th "Insurance number"]
      [:th "Actions"]]]
    [:tbody
     (for [{:keys [id, full_name, sex, birthday, address, insurance_number]} @patients]
       [:tr {:key id}
        [:td full_name]
        [:td sex]
        [:td "birthday"]
        [:td address]
        [:td insurance_number]
        [:td [:button.button {:on-click #((delete-patient id) patients)} "Delete"]]])
     ]]])

;; TODO update patients atom after successful operation
(defn add-patient! [fields errors]
  (println @fields)
  (POST "/patient"
        {:format        :json
         :headers
         {"Accept"       "application/transit+json"
          "x-csrf-token" (. js/window -csrfToken)}
         :params        @fields
         :handler       #(do
                           (.log js/console (str "response:" %))
                           (reset! errors nil))
         :error-handler #(do
                           (.log js/console (str %))
                           (reset! errors (get-in % [:response :errors])))}))

(defn errors-component [errors id]
  (when-let [error (id @errors)]
    [:div.notification.is-danger (string/join error)]))

(defn patients-form []
  (let [fields         (r/atom {})
        errors         (r/atom nil)
        change-handler (fn [field]
                         (fn [event]
                           (let [value (-> event .-target .-value)]
                             (swap! fields assoc field value))))]
    (fn []
      [:div
       [errors-component errors :server-error]
       [:div.field
        [:label.label {:for :full_name} "Full name"]
        [errors-component errors :full_name]
        [:input.input
         {:type      :text
          :name      :full_name
          :on-change (change-handler :full_name)
          :value     (:full_name @fields)}]]
       [:div.field
        [:label.label "Sex"]
        [errors-component errors :sex]
        [:div.control
         [:label.radio
          [:input {:type      "radio"
                   :name      "sex"
                   :value     "male"
                   :on-change (change-handler :sex)
                   :checked   (= (:sex @fields) "male")}] "Male"]
         [:label.radio
          [:input {:type      "radio"
                   :name      "sex"
                   :value     "female"
                   :on-change (change-handler :sex)
                   :checked   (= (:sex @fields) "female")}] "Female"]
         ]]
       [:div.field
        [:label.label {:for :birthday} "Birthday"]
        [errors-component errors :birthday]
        [:input.input
         {:type      :date
          :name      :birthday
          :on-change (change-handler :birthday)
          :value     (:birthday @fields)}]]
       [:div.field
        [:label.label {:for :address} "Address"]
        [errors-component errors :address]
        [:input.input
         {:type      :text
          :name      :address
          :on-change (change-handler :address)
          :value     (:address @fields)}]]
       [:div.field
        [:label.label {:for :insurance_number} "Insurance number"]
        [errors-component errors :insurance_number]
        [:input.input
         {:type      :text
          :name      :insurance_number
          :on-change (change-handler :insurance_number)
          :value     (:insurance_number @fields)}]]
       [:input.button.is-primary
        {:type     :submit
         :on-click #(add-patient! fields errors)
         :value    "Add"}]])))

(defonce patients (r/atom nil))
(get-patients patients)

(defn home []
  (fn []
    [:div.content>div.columns.is-centered>div.column.is-two-thirds
     [:div.columns>div.column
      [:h3 "Patients list"]
      [patients-list patients]]
     [:div.columns>div.column
      [patients-form]]]))

(defn mount-components []
(rd/render [home]  (.getElementById js/document "content")))

(defn init! []
(mount-components))
