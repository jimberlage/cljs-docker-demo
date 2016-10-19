(ns docker-demo.core
  (:require [cljs.core.async :refer [<!]]
            [cljs.reader :refer [read-string]]
            [cljs-http.client :as http]
            [docker-demo.env :as env]
            [reagent.core :as reagent])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def server
  (str "http://" env/host ":" env/port))

(def db
  (reagent/atom {:apps []
                 :new-app {:env ""
                           :path ""
                           :port ""}
                 :next-id 0}))

(def id
  (reagent/cursor db [:next-id]))

(defn get-id
  "Returns a unique id for an app."
  []
  (let [result (atom nil)]
    (swap! id (fn [id]
                ;; Store the current value of the db.
                (reset! result id)
                ;; Increment the id.
                (inc id)))
    @result))

(defn modify-app
  ""
  [app app-atom api-path]
  (go
    (let [response (<! (http/put (str server api-path) {:edn-params app}))]
      (if (= (:status response) 200)
        (reset! app-atom (:body response))
        (js/console.error (:body response)))))
  app)

(defn app-overview-component
  ""
  [app-atom]
  (let [app @app-atom]
    [:div.form.form-horizontal
     [:div.form-group
      [:label.control-label "Path"]
      [:input {:class-name "form-control"
               :default-value (:path app)
               :on-change (fn [event]
                            (swap! app-atom assoc :path (-> event .-target .-value)))
               :type "text"}]]
     [:div.form-group
      [:label.control-label "Port"]
      [:input {:class-name "form-control"
               :default-value (:port app)
               :on-change (fn [event]
                            (swap! app-atom assoc :port (-> event .-target .-value)))
               :type "text"}]]
     [:div.form-group
      [:label.control-label "Environment"]
      [:textarea {:class-name "form-control"
                  :default-value (:env app)
                  :on-change (fn [event]
                               (swap! app-atom assoc :env (-> event .-target .-value)))}]]
     [:div.form-group
      [:label.control-label "Image ID"]
      [:p.form-control-static (or (:image-id app) "N/A")]]
     [:div.form-group
      [:label.control-label "Container ID"]
      [:p.form-control-static (or (:container-id app) "N/A")]]
     [:div.form-group
      [:button {:class-name "btn btn-small btn-primary"
                :on-click (fn [_]
                            (modify-app app app-atom "/api/apps/build"))}
       "Build"]
      [:button {:class-name "btn btn-small btn-primary"
                :on-click (fn [_]
                            (modify-app app app-atom "/api/apps/run"))}
       "Run"]
      [:button {:class-name "btn btn-small btn-primary"
                :on-click (fn [_]
                            (modify-app app app-atom "/api/apps/stop"))}
       "Stop"]]
     [:pre (pr-str app)]]))

(defn apps-overview-component
  ""
  []
  (let [apps-atom (reagent/cursor db [:apps])]
    (fn []
      (let [apps @apps-atom]
        [:div
         [:h4 "Apps"]
         [:div.container-fluid
          (map-indexed (fn [i app]
                         ^{:key (:id app)}
                         [app-overview-component (reagent/cursor db [:apps i])])
                       apps)]]))))

(defn add-app-input-component
  ""
  []
  (let [new-app-atom (reagent/cursor db [:new-app])]
    (fn []
      (let [new-app @new-app-atom]
        [:div
         [:h4 "New App"]
         [:div.container-fluid
          [:div.form.form-horizontal
           [:div.form-group
            [:label.control-label "Path"]
            [:input {:class-name "form-control"
                     :on-change (fn [event]
                                  (swap! new-app-atom assoc :path (-> event .-target .-value)))
                     :type "text"
                     :value (:path new-app)}]]
           [:div.form-group
            [:label.control-label "Port"]
            [:input {:class-name "form-control"
                     :on-change (fn [event]
                                  (swap! new-app-atom assoc :port (-> event .-target .-value)))
                     :type "text"
                     :value (:port new-app)}]]
           [:div.form-group
            [:label.control-label "Environment"]
            [:textarea {:class-name "form-control"
                        :on-change (fn [event]
                                     (swap! new-app-atom assoc :env (-> event .-target .-value)))}]]
           [:div.form-group
            [:button {:class-name "btn btn-small btn-primary"
                      :on-click (fn [event]
                                  (.preventDefault event)
                                  (let [app-id (get-id)
                                        app (assoc new-app :id app-id)]
                                    (swap! db update :apps conj app)))}
             "Create"]]]]]))))

(defn app-component
  "The parent component for the app."
  []
  [:div.container-fluid
   [apps-overview-component]
   [add-app-input-component]])

(defn main
  ""
  []
  (reagent/render [app-component] (js/document.getElementById "mnt")))

(main)
