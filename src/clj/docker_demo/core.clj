(ns docker-demo.core
  (:gen-class)
  (:require [clojure.edn :as edn]
            [clojure.java.shell :refer [sh]]
            [clojure.string :refer [split-lines trim-newline]]
            [clojure.core.async :refer [go]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.file :refer [wrap-file]]
            [ring.util.response :refer [file-response]]))

(defn apps-handler
  ""
  [request f]
  (let [app (edn/read-string (slurp (:body request)))
        [app error] (f app)]
    (if error
      {:status 500
       :headers {"Content-Type" "application/edn"}
       :body (pr-str error)}
      {:status 200
       :headers {"Content-Type" "application/edn"}
       :body (pr-str app)})))

(defn build-app
  "Builds an image for the given app."
  [app]
  (let [result (sh "docker" "build" "--quiet" (:path app))]
    (if (= (:exit result) 0)
      ;; Update the app with the returned image id.
      (let [app (assoc app :image-id (trim-newline (:out result)))]
        [app nil])
      ;; Combine stdout and stderr when displaying the error.
      (let [error (str (:out result) \newline (:err result))]
        [app error]))))

(defn run-app
  "Runs a container for the given app.  Builds an image for the app if an image does not already exist."
  ([app image-id]
   (let [args ["docker" "run" "--detach" "--publish" (str "127.0.0.1:" (:port app) ":3000/tcp")]
         env (split-lines (:env app))
         env (map (fn [env-var]
                    ["--env" env-var])
                  env)
         env (flatten env)
         args (concat args env)
         args (vec args)
         args (conj args image-id)
         _ (println (pr-str args))
         result (apply sh args)]
     (if (= (:exit result) 0)
       ;; Update the app with the returned container id.
       (let [app (assoc app :container-id (trim-newline (:out result)))]
         [app nil])
       ;; Combine stdout and stderr when displaying the error.
       (let [error (str (:out result) \newline (:err result))]
         [app error]))))
  ([app]
   (if-let [image-id (:image-id app)]
     (run-app app image-id)
     (let [[app error] (build-app app)]
       (run-app app (:image-id app))))))

(defn stop-app
  ""
  [app]
  (if (:container-id app)
    (let [result (sh "docker" "stop" (:container-id app))]
      (if (= (:exit result) 0)
        (let [app (dissoc app :container-id)]
          [app nil])
        ;; Combine stdout and stderr when displaying the error.
        (let [error (str (:out result) \newline (:err result))]
          [app error])))
    [app nil]))

(defn router
  "Dispatches to different handlers based on the method and path of each request."
  [handler]
  (fn [request]
    (cond
      (and (= :get (:request-method request)) (= "/" (:uri request)))
      (assoc-in (file-response "index.html" {:root "frontend/html"}) [:headers "Content-Type"] "text/html")

      (and (= :put (:request-method request)) (= "/api/apps/build" (:uri request)))
      (apps-handler request build-app)

      (and (= :put (:request-method request)) (= "/api/apps/run" (:uri request)))
      (apps-handler request run-app)

      (and (= :put (:request-method request)) (= "/api/apps/stop" (:uri request)))
      (apps-handler request stop-app)

      :else
      (handler request))))

(defn not-found
  "Serves a 404."
  [_]
  {:status 404
   :headers {"Content-Type" "text/plain"}
   :body "Not Found"})

(defn -main
  "Runs a simple web server to deliver our clojurescript app to the browser."
  [& args]
  (run-jetty (-> not-found
                 (router)
                 (wrap-file "frontend") ;; Serve html, css, javascript as-is.
                 (wrap-content-type))   ;; Ensure the content-type is correct.
             {:port 3000}))
