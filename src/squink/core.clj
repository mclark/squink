(ns squink.core
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.string :refer [blank? trim]]
            [clojure.edn :as edn]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [compojure.core :refer [defroutes GET POST]]
            [clojurewerkz.urly.core :refer [url-like]]
            [ring.adapter.jetty :refer [run-jetty]]
            [squink.hash :refer [hash128]])
  (:import java.sql.SQLIntegrityConstraintViolationException
           java.io.PushbackReader)
  (:gen-class))

(declare config)

(def memoized (atom {}))

(def db {:subprotocol "mysql"
         :subname     "//localhost:3306/squink"
         :user        "root"})

(defn create-schema [erase-existing]
  (when erase-existing (jdbc/db-do-commands db "DROP TABLE IF EXISTS shortened_url"))
  (jdbc/db-do-commands
    db
    "CREATE TABLE IF NOT EXISTS `shortened_url` (
      `id` int(11) NOT NULL AUTO_INCREMENT,
      `hashed` CHAR(8) NOT NULL,
      `url` VARCHAR(2000) NOT NULL,
      `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
      PRIMARY KEY (`id`),
      UNIQUE KEY (`hashed`)) ENGINE=InnoDB DEFAULT CHARSET=utf8"))

(defn find-url [conn hashed]
  (first (jdbc/query conn ["SELECT url FROM shortened_url WHERE hashed=?" hashed]
                     :row-fn :url)))

(defn insert-url [conn url hashed]
  (try
    (jdbc/insert! conn :shortened_url
                  {:url url :hashed hashed})
    hashed
    (catch SQLIntegrityConstraintViolationException e
      (when (= url (lookup-url hashed)) hashed))))

(defn lookup-url [code]
  (if-let [url (get @memoized code)]
    url
    (if-let [url (find-url db code)]
      (do (swap! memoized
                 (fn [m] (if-not (get m code) (assoc m code url) m)))
          (get @memoized code)))))

(defn persist [url hashed]
  (loop [char-count 1]
    (let [h (apply str (take char-count hashed))
          existing (lookup-url h)]
      (if (nil? existing)
        (or (insert-url db url h) (if (< char-count (count hashed)) (recur (inc char-count)) nil))
        (if (= existing url) h (if (< char-count (count hashed)) (recur (inc char-count)) nil))))))

(defn shorten [url]
  (loop [candidate-url url tries-remaining 3]
    (let [hashed (hash128 candidate-url)
          persisted-hash (persist candidate-url hashed)]
      (if (and (nil? persisted-hash)
               (< 0 tries-remaining))
        (recur (str tries-remaining "+" url) (dec tries-remaining))
        persisted-hash))))

(defn- sanitize-url [^String url]
  (if (blank? url)
    nil
    (.toString (url-like url))))

(defn- valid? [^String url]
  (and (not (blank? url)) (> 2000 (.length url))))

(defn handle-create [url]
  (let [cleaned-url (sanitize-url url)]
    (if (valid? cleaned-url)
      (if-let [shortened (shorten cleaned-url)]
        {:status 201
         :body shortened}
        {:status 400 :body "uncomputable hash!"})
      {:status 400 :body "invalid url"})))

(defn handle-lookup [hash]
  (let [url (find-url db hash)]
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

(defn read-config []
  (def config (edn/read-string (slurp "squink.conf.edn")))
  (def db (:db config)))

(defn -main [& args]
  (read-config)
  (create-schema false)
  (run-jetty handler {:port 80}))