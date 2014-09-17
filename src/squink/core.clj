(ns squink.core
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.string :refer [blank?]]
            [ring.middleware.params :refer [wrap-params]]
            [compojure.core :refer [defroutes GET POST]])
  (:import clojure.lang.Murmur3))

(def db {:subprotocol "mysql"
         :subname "//localhost:3306/squink"
         :user "root"})

(defn create-schema []
  (jdbc/db-do-commands
    db
    "DROP TABLE IF EXISTS shortened_url"
    "CREATE TABLE `shortened_url` (
      `id` int(11) NOT NULL AUTO_INCREMENT,
      `hashed` CHAR(8) NOT NULL,
      `url` VARCHAR(2000) NOT NULL,
      `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
      PRIMARY KEY (`id`),
      UNIQUE KEY (`hashed`)) ENGINE=InnoDB DEFAULT CHARSET=utf8"))

(def hostname "squi.nk")

(defn find-url [hashed]
  (first (jdbc/query db ["SELECT url FROM shortened_url WHERE hashed=?" hashed]
                     :row-fn :url)))

(defn insert-url [url hashed]
            (jdbc/insert! db :shortened_url
                          {:url url :hashed hashed})
  hashed)

(def hash-url (comp (partial format "%x")
                    #(Murmur3/hashUnencodedChars %)))

(defn shorten [url]
  (loop [candidate-url url tries-remaining 3]
    (let [hashed (hash-url candidate-url)
          existing (find-url hashed)]
      (if (nil? existing)
        (insert-url url hashed)
        (if (= existing url)
          hashed
          (when
            (< 0 tries-remaining)
            (recur (str tries-remaining "+" url) (dec tries-remaining))))))))

(defn valid? [url]
  (and (not (blank? url)) (> 2000 (.length url))))

(defroutes app
           (GET "/:hash" [hash]
                (let [url (find-url hash)]
                  (if url {:status 301 :headers {"Location" url}}
                          {:status 404})))
           (POST "/" {{url "url"} :params}
                 (if (valid? url)
                   {:status 201
                    :body   (str hostname "/" (shorten url))}
                   {:status 400})))

(def handler
  (-> app
      wrap-params))