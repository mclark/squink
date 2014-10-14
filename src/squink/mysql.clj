(ns squink.mysql
  (:require [clojure.java.jdbc :as jdbc])
  (:import java.sql.SQLIntegrityConstraintViolationException))

(def db {:subprotocol "mysql"
         :subname     "//localhost:3306/squink"
         :user        "root"})

(def memoized (atom {}))

(defn create-schema [db erase-existing]
  (when erase-existing (jdbc/db-do-commands db "DROP TABLE IF EXISTS shortened_url"))
  (jdbc/db-do-commands
    db
    "CREATE TABLE IF NOT EXISTS `shortened_url` (
      `id` int(11) NOT NULL AUTO_INCREMENT,
      `hashed` VARCHAR(32) NOT NULL,
      `url` VARCHAR(2000) NOT NULL,
      `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
      PRIMARY KEY (`id`),
      UNIQUE KEY (`hashed`)) ENGINE=InnoDB DEFAULT CHARSET=utf8"))

(defn find-url [conn hashed]
  (first (jdbc/query conn ["SELECT url FROM shortened_url WHERE hashed=?" hashed]
                     :row-fn :url)))

(defn lookup-url [conn code]
    (if-let [url (get @memoized code)]
      url
      (if-let [url (find-url conn code)]
        (get (swap! memoized
                    (fn [m] (if-not (get m code) (assoc m code url) m)))
             code))))

(defn insert-url [conn url hashed]
  (try
    (jdbc/insert! conn :shortened_url
                  {:url url :hashed hashed})
    url
    (catch SQLIntegrityConstraintViolationException e
      (when (= url (lookup-url conn hashed)) url))))