(ns test-health-samurai.core
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [re-frame.db :as rfd]
            [reagent.dom :as rd]
            [ajax.core :refer [GET POST DELETE PATCH]]
            [clojure.string :as string]))

;; Helpers
(defn classnames [& args]
  (string/join " " (map name (filter identity args))))

(defn log [& args]
  (apply (.-log js/console) args))

;; state helpers
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

(rf/reg-event-db
  :patients/update
  (fn [db [_ patient]]
    (log patient)
    (update db :patients/list (fn [list] (map #(if (= (:id %) (:id patient))
                                                 patient
                                                 %)
                                              list)))))

(rf/reg-sub
  :patients/list
  (fn [db _]
    (:patients/list db [])))

(rf/reg-sub
  :modal/visible?
  (fn [db _]
    (get-in db [:modal :visible?] false)))

(rf/reg-event-db
  :modal/close
  (fn [db [_ _]]
    (assoc-in db [:modal :visible?] false)))

(rf/reg-event-db
  :modal/open
  (fn [db [_ type]]
    (update-in db [:modal] assoc :visible? true :type type)))

(rf/reg-sub
  :form/fields
  (fn [db _]
    (get-in db [:form/fields] {})))

(rf/reg-event-db
  :form/change
  (fn [db [_ field data]]
    (update db :form/fields assoc field data)))

(rf/reg-event-db
  :form/clear
  (fn [db [_ _]]
    (assoc db :form/fields {})))

(rf/reg-event-db
  :form/fill
  (fn [db [_ patient]]
    (assoc db :form/fields patient)))

(rf/reg-sub
  :modal/type
  (fn [db _]
    (log db)
    (get-in db [:modal :type] :add-new)))

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

(defn add-patient! [fields errors]
  (POST "/patient"
        {:format        :json
         :headers
         {"Accept"       "application/transit+json"
          "x-csrf-token" (. js/window -csrfToken)}
         :params        @fields
         :handler       #(do
                           (rf/dispatch [:patients/add @fields])
                           (rf/dispatch [:form/clear])
                           (rf/dispatch [:modal/close])
                           (.log js/console (str "response:" %))
                           (reset! errors nil))
         :error-handler #(do
                           (.log js/console (str %))
                           (reset! errors (get-in % [:response :errors])))}))

(defn edit-patient! [fields errors]
  (PATCH (string/join "/" ["/patients" (:id @fields)])
         {:format        :json
          :headers       {"Accept"       "application/transit+json"
                          "x-csrf-token" (. js/window -csrfToken)}
          :params        @fields
          :handler       #(do
                            (rf/dispatch [:patients/update @fields])
                            (rf/dispatch [:form/clear])
                            (rf/dispatch [:modal/close])
                            (.log js/console (str "response: " %))
                            (reset! errors nil))
          :error-handler #(do
                            (.log js/console (str %))
                            (reset! errors (get-in % [:response :errors])))}))

(defn open-edit-modal [id]
  (let [patients @(rf/subscribe [:patients/list])
        patient  (first (filter #(= (:id %) id) patients))]
    (rf/dispatch [:form/fill patient])
    (rf/dispatch [:modal/open :edit])))

;; TODO do not display if patients list is empty
(defn patients-list [patients]
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
        [:td birthday]
        [:td address]
        [:td insurance_number]
        [:td
         [:div.buttons
          [:button.button {:on-click #(delete-patient id)} [:span.icon [:i.mi.mi-delete]]] ;;TODO add confirmation before delete
          [:button.button {:on-click #(open-edit-modal id)} [:span.icon [:i.mi.mi-edit]]]]]])]]])

(defn errors-component [errors id]
  (when-let [error (id @errors)]
    [:div.notification.is-danger (string/join error)]))

(defn patients-form [submit-handler]
  (let [fields         (rf/subscribe [:form/fields])
        errors         (r/atom nil)
        change-handler (fn [field]
                         (fn [event]
                           (let [value (-> event .-target .-value)]
                             (rf/dispatch [:form/change field value]))))]
    (fn []
      [:form {:on-submit (fn [e] (.preventDefault e)
                           (submit-handler fields errors))}
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
          [:input.mr-1 {:type      "radio"
                        :name      "sex"
                        :value     "male"
                        :on-change (change-handler :sex)
                        :checked   (= (:sex @fields) "male")}] "Male"]
         [:label.radio
          [:input.mr-1 {:type      "radio"
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
       [:div.buttons
        [:input.button.is-primary
         {:type  :submit
          :value "Add"}]
        [:button.button.is-danger {:on-click (fn [e] (.preventDefault e) (rf/dispatch [:modal/close]))} "Cancel"]]])))

(defn open-modal []
  (rf/dispatch [:modal/open]))

(defn patient-modal []
  (let [visible?       (rf/subscribe [:modal/visible?])
        close-handler  (fn [] (rf/dispatch [:modal/close]))
        type           (rf/subscribe [:modal/type])
        submit-handler (fn [fields errors] (if (= @type :edit)
                                             (edit-patient! fields errors)
                                             (add-patient! fields errors)))]
    (fn []
      (let [title (if (= @type :edit) "Edit patient" "Add patient")]
        [:div.modal {:class (classnames (when @visible? "is-active"))}
         [:div.modal-background {:on-click close-handler}]
         [:div.modal-card
          [:header.modal-card-head
           [:p.modal-card-title title]]
          [:section.modal-card-body
           [patients-form submit-handler]]]
         [:button.modal-close.is-large {:aria-label :close :on-click close-handler}]]))))

(defn home []
  (let [patients (rf/subscribe [:patients/list])]
    (rf/dispatch [:app/initialize])
    (get-patients)
    (fn []
      [:div.content.mt-5>div.columns.is-centered>div.column.is-two-thirds
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
