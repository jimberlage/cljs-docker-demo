(ns docker-demo.core
  (:gen-class)
  (:require [clojure.java.shell :refer [sh]]
            [core.async :refer [go]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.file :refer [wrap-file]]
            [ring.util.response :refer [file-response]]))

;; A container storing state on the server.  We're keeping all data in-memory, since persistence isn't a huge issue for
;; this app.  However, this does mean that we need to be careful about what processes we launch - we should use a
;; consistent naming scheme for docker containers, so they can be cleaned up if the app is terminated unexpectedly.
(def db (atom {:id 0
               :containers {}}))

(defn get-id
  "Returns a unique id for a container.  This will help us identify the request to start a container even if the process has not started or the container has not spun up yet."
  []
  (let [id (atom nil)]
    (swap! db (fn [db]
                ;; Store the current value of the db.
                (reset! id (:id db))
                ;; Increment the id.
                (update db :id inc)))
    @id))

(defn start-app
  "Runs a docker image for the provided app."
  [path]
  (let [id (get-id)]
    (go
      (sh ))))

(defn not-found
  "Serves a 404."
  [request]
  {:status 404
   :headers {"Content-Type" "text/plain"}
   :body "Not Found"})

(defn wrap-root
  ""
  [handler]
  (fn [req]
    (if (= "/" (:uri req))
      (assoc-in (file-response "index.html" {:root "frontend/html"}) [:headers "Content-Type"] "text/html")
      (handler req))))

(defn -main
  "Runs a simple web server to deliver our clojurescript app to the browser."
  [& args]
  (run-jetty (-> not-found
                 (wrap-file "frontend") ;; Serve html, css, javascript as-is.
                 (wrap-root)            ;; Redirect / to /index.html.
                 (wrap-content-type))   ;; Ensure the content-type is correct.
             {:port 3000}))
