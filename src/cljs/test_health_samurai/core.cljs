(ns test-health-samurai.core
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [re-frame.db :as rfd]
            [reagent.dom :as rd]
            [ajax.core :refer [GET POST DELETE]]
            [clojure.string :as string]))

;; Helpers
(defn classnames [& args]
  (string/join " " (map name (filter identity args))))

(rf/reg-event-fx
  :app/initialize
  (fn [_ _]
    {:db {:patients/loading? true}}))

(rf/reg-sub
  :patients/loading?
  (fn [db _]
    (:patients/loading? db)))

(rf/reg-event-db
  :patients/set
  (fn [db [_ patients]]
    (-> db
        (assoc :patients/loading? false
               :patients/list patients))))

(rf/reg-event-db
  :patients/add
  (fn [db [_ patient]]
    (update db :patients/list conj patient)))

(rf/reg-event-db
  :patients/delete
  (fn [db [_ id]]
    (update db :patients/list (partial remove #(= (:id %) id)))))

(rf/reg-sub
  :patients/list
  (fn [db _]
    (:patients/list db [])))

(rf/reg-sub
  :modal/visible?
  (fn [db _]
    (:modal/visible? db)))

(defn log [& args]
  (apply (.-log js/console) args))

(rf/reg-event-db
  :modal/close
  (fn [db [_ _]]
    (do
      (log db)
      (assoc db :modal/visible? false))))

(rf/reg-event-db
  :modal/open
  (fn [db [_ _]]
    (do
      (log db)
      (assoc db :modal/visible? true))))

(defn get-patients []
  (GET "/patients"
       {:headers {"Accept" "application/transit+json"}
        :handler #(rf/dispatch [:patients/set (:patients %)])}))

(log (:patients/list @rfd/app-db))

(defn delete-patient [id]
  (DELETE "/patients"
          {:format        :json
           :headers
           {"Accept"       "application/transit+json"
            "x-csrf-token" (. js/window -csrfToken)}
           :params        {:id id}
           :handler       #(do
                             (log %)
                             (rf/dispatch [:patients/delete id]))
           :error-handler #(do
                             (log %))})) ;; TODO add toast notification

(defn edit-patient [id]
  (rf/dispatch [:modal/open]))

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
        [:td
         [:button.button {:on-click #(delete-patient id)} "Delete"]
         [:button.button {:on-click #(edit-patient id)} "Edit"]]])]]])

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
                           (rf/dispatch [:patients/add @fields])
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
                   :checked   (= (:sex @fields) "female")}] "Female"]]]
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

(defn display-modal [e]
  (log e))

(defn open-modal []
  (rf/dispatch [:modal/open]))

;; TODO figure out how to use re-frame, in order to easier manage state of open/closed modal
(defn patient-modal []
  (let [visible?    (rf/subscribe [:modal/visible?])
        close-modal (fn [] (rf/dispatch [:modal/close]))]
    (fn []
      [:div.modal {:class (classnames (when @visible? "is-active"))}
       [:div.modal-background {:on-click close-modal}]
       [:div.modal-card
        [:header.modal-card-head
         [:p.modal-card-title "Add patient"]]
        [:section.modal-card-body
         [patients-form]]]
       [:button.modal-close.is-large {:aria-label :close :on-click close-modal}]])))

(defn home []
  (let [patients (rf/subscribe [:patients/list])]
    (rf/dispatch [:app/initialize])
    (get-patients)
    (fn []
      [:div.content>div.columns.is-centered>div.column.is-two-thirds
       (if @(rf/subscribe [:patients/loading?])
         [:h3 "Loading patients..."]
         [:div.columns>div.column
          [:h3 "Patients list"]
          [patients-list patients]
          [:div.buttons [:button.button.is-primary {:on-click open-modal}  "Add patient"]]
          [patient-modal]])])))

(defn mount-components []
  (rd/render [home]  (.getElementById js/document "content")))

(defn init! [] (mount-components))
