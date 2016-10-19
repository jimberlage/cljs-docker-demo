(ns docker-demo.core
  (:require [cljs.core.async :refer [<!]]
            [cljs.reader :refer [read-string]]
            [cljs-http.client :as http]
            [docker-demo.env :as env]
            [reagent.core :as reagent])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def server
  (str "http://" env/host ":" env/port))

;; This is our state object.  Similar to [Redux](http://redux.js.org/), we use a single object to store state.  Unlike
;; Redux, we do not have a tree of handlers.  Instead, you should just update the state with plain Clojure functions and
;; the usual methods for updating atoms, swap! and reset!.
(def db
  (reagent/atom {;; A list of all the apps we are keeping track of.
                 :apps []
                 ;; Data for a new app - when the user fills in the form to add a new app to be run, it goes here.
                 :new-app {:env ""
                           :path ""
                           :port ""}
                 ;; A counter so we can give each app a unique id.
                 :next-id 0}))

(def id
  ;; Reagent cursors return a new atom, at a subpath in the state.  It is generally more efficient to use these on
  ;; large state trees, since they do not trigger changes to components listening to other parts of the tree.
  (reagent/cursor db [:next-id]))

(defn get-id
  "Returns a unique id for an app."
  []
  (let [result (atom nil)]
    ;; We need to take the value of the id in a transaction, to prevent changes in between the time we read the value
    ;; and the time we increment it.
    (swap! id (fn [id]
                ;; Store the current value of the db.
                (reset! result id)
                ;; Increment the id.
                (inc id)))
    @result))

(defn modify-app
  "Calls the api at a given path, with the given app.  The app-atom will be updated with any changes if the API call is successful."
  [app app-atom api-path]
  (go
    (let [response (<! (http/put (str server api-path) {:edn-params app}))]
      (if (= (:status response) 200)
        (reset! app-atom (:body response))
        (js/console.error (:body response)))))
  app)

;; A Reagent component.  This would be
;;
;; ```javascript
;; class AppOverviewComponent extends Component {
;;   render() {
;;     const app = this.state.app;
;;     return <div className="form form-horizontal"></div>;
;;   }
;; }
;; ```
;;
;; in plain React.
(defn app-overview-component
  "A React component to display a single app.  The path, port, and environment for the app may be edited from here, and the app may be built, run, or stopped."
  [app-atom]
  ;; Dereference the atom only once.
  (let [app @app-atom]
    ;; Reagent/Hiccup allows you to specify classes this way, instead of writing {:class-name "form form-horizontal"}.
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
       "Stop"]]]))

;; This component is a little different - it returns a function instead of HTML-as-hiccup.
;; This means that the returned function is the render() method.
;;
;; In javascript/React:
;;
;; ```javascript
;; class AppsOverviewComponent extends Component {
;;   constructor() {
;;     this.state.apps = DB.apps;
;;   }
;;
;;   render() {
;;     const apps = this.state.apps;
;;     return <div></div>;
;;   }
;; }
;; ```
(defn apps-overview-component
  "Displays all the apps in the state."
  []
  ;; Get a cursor to just the :apps section of the state, to avoid triggering needless updates to other components.
  (let [apps-atom (reagent/cursor db [:apps])]
    (fn []
      (let [apps @apps-atom]
        [:div
         [:h4 "Apps"]
         [:div.container-fluid
          (map-indexed (fn [i app]
                         ;; Lists in React require each element to have a `key` prop.  In reagent, we supply it as
                         ;; metadata on the child component.
                         ^{:key (:id app)}
                         [app-overview-component (reagent/cursor db [:apps i])])
                       apps)]]))))

(defn add-app-input-component
  "A form to add a new app to the list."
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
   ;; When debugging, it's often helpful to take a look at the whole state.
   #_[:pre (pr-str @db)]
   [apps-overview-component]
   [add-app-input-component]])

(defn main
  "Run the app."
  []
  (reagent/render [app-component] (js/document.getElementById "mnt")))

;; Unlike on the JVM, we need to call main explicitly.
(main)
