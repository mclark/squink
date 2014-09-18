(ns squink.core
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.string :refer [blank? trim]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [compojure.core :refer [defroutes GET POST]]
            [clojurewerkz.urly.core :refer [url-like]])
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

(defn find-url [conn hashed]
  (first (jdbc/query conn ["SELECT url FROM shortened_url WHERE hashed=?" hashed]
                     :row-fn :url)))

(defn insert-url [conn url hashed]
            (jdbc/insert! conn :shortened_url
                          {:url url :hashed hashed})
  true)

(def hash-url (comp (partial format "%x")
                    #(Murmur3/hashUnencodedChars %)))

(defn persist [url hashed]
  (jdbc/with-db-transaction
    [conn db]
    (let [existing (find-url conn hashed)]
      (if (nil? existing) (insert-url conn url hashed) (= existing url)))))

(defn shorten [url]
  (loop [candidate-url url tries-remaining 3]
    (let [hashed (hash-url candidate-url)
          synced-to-db? (persist candidate-url hashed)]
      (if (and (< 0 tries-remaining) (not synced-to-db?))
        (recur (str tries-remaining "+" url) (dec tries-remaining))
        hashed))))

(defn- sanitize-url [^String url]
  (if (blank? url)
    nil
    (.toString (url-like url))))

(defn- valid? [url]
  (and (not (blank? url)) (> 2000 (.length url))))

(defn handle-create [url]
  (let [cleaned-url (sanitize-url url)]
    (if (valid? cleaned-url)
      {:status 201
       :body   (str hostname "/" (shorten cleaned-url))}
      {:status 400})))

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