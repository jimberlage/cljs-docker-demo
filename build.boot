(set-env! :dependencies '[[adzerk/boot-cljs "1.7.228-1" :scope "test"]
                          [adzerk/boot-reload "0.4.12" :scope "test"]
                          [cljs-http "0.1.11"]
                          [javax.servlet/servlet-api "2.5" :scope "clj"]
                          [org.clojure/clojure "1.8.0" :scope "clj"]
                          [org.clojure/clojurescript "1.9.229" :scope "test"]
                          [org.clojure/core.async "0.2.395" :scope "clj"]
                          [reagent "0.6.0"]
                          [ring/ring-core "1.5.0" :scope "clj"]
                          [ring/ring-jetty-adapter "1.5.0" :scope "clj"]]
          :resource-paths #{"resources"}
          :source-paths #{"src/clj" "src/cljs"})

(require '[adzerk.boot-cljs :refer [cljs]]
         '[adzerk.boot-reload :refer [reload]])

(deftask backend
  "Packages up our app as a JAR."
  []
  (comp
    ;; Compile clojure namespaces.
    (aot :namespace #{'docker-demo.core})
    ;; Pull in dependencies.
    (uber :include-scope #{"clj"})
    ;; Generate a JAR file.
    (jar :main 'docker-demo.core)
    ;; Keep just the JAR file.
    (sift :include #{#"^project\.jar$"})
    (target :dir #{"target/backend"})))

(deftask frontend-dev
  "Compiles clojurescript to javascript for development, with source maps and other cool stuff."
  []
  (comp
    ;; Look for changes in files.
    (watch)
    ;; Reload clojurescript when the underlying files change.
    (reload)
    ;; Compile clojurescript.  By default, this will have :optimizations :none and :source-maps true.
    (cljs)
    (target :dir #{"target/frontend"})))

(deftask frontend-prod
  ""
  []
  (comp
    ;; Compile clojurescript.
    ;;
    ;; This is my preferred set of options for production CLJS.  For more compiler options, check here:
    ;; https://github.com/clojure/clojurescript/wiki/Compiler-Options
    (cljs :compiler-options {:compiler-stats true
                             :parallel-build true
                             :verbose true}
          :optimizations :advanced)
    (target :dir #{"target/frontend"})))
