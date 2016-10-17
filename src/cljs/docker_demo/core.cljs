(ns docker-demo.core
  (:require [reagent.core :as reagent]))

(defn apps-overview-component
  ""
  []
  [:div "I show the status of all the apps."])

(defn add-app-input-component
  ""
  []
  [:div "I add an app to the list."])

(defn app-component
  "The parent component for the app."
  []
  [:div
   [apps-overview-component]
   [add-app-input-component]])

(defn main
  ""
  []
  (reagent/render [app-component] (js/document.getElementById "mnt")))

(main)
