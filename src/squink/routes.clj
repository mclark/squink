(ns squink.routes
  (:require [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [compojure.core :refer [defroutes GET POST]]
            [ring.adapter.jetty :refer [run-jetty]]
            [squink.core :refer [sanitize-url valid? shorten set-config! *db*]]
            [squink.mysql :refer [lookup-url create-schema!]])
  (:gen-class))

(defn handle-create [url]
  (let [cleaned-url (sanitize-url url)]
    (if (valid? cleaned-url)
      (if-let [shortened (shorten cleaned-url)]
        {:status 201
         :body shortened}
        {:status 400 :body "uncomputable hash!"})
      {:status 400 :body "invalid url"})))

(defn handle-lookup [hash]
  (let [url (lookup-url *db* hash)]
    (if url {:status 301 :headers {"Location" url}}
            {:status 404})))

(defroutes app
           (GET "/:hash" [hash] (handle-lookup hash))
           (POST "/" {{url :url} :params}
                 (handle-create url)))

(def handler
  (-> app
      wrap-keyword-params
      wrap-params))

(defn -main [& args]
  (set-config!)
  (create-schema! *db* false)
  (run-jetty handler {:port 80}))
