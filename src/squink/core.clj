(ns squink.core
  (:require [squink.mysql :refer [lookup-url insert-url]]
            [squink.hash :refer [hash128]]
            [clojure.string :refer [blank? trim]]
            [clojurewerkz.urly.core :refer [url-like]]
            [clojure.edn :as edn]))

(def ^:dynamic *config* {:retry-count 3 :base-stem 2})

(def ^:dynamic *db* {:subprotocol "mysql"
                     :subname     "//mysql:3306/squink"
                     :user        "root"})

(defn- persist [url hashed]
  (loop [char-count (:base-stem *config*)]
    (let [h (apply str (take char-count hashed))
          existing (or (lookup-url *db* h) (insert-url *db* url h))]
      (if (= existing url)
        h
        (if (< char-count (count hashed))
          (recur (inc char-count))
          nil)))))

(defn- prepend-attempt [attempt url]
  (if (zero? attempt) url (str attempt "+" url)))

(defn shorten [url]
  (loop [attempt 0]
    (let [hashed (hash128 (prepend-attempt attempt url))
          persisted-hash (persist url hashed)]
      (if (and (nil? persisted-hash)
               (< attempt (:retry-count *config*)))
        (recur (inc attempt))
        persisted-hash))))

(defn sanitize-url [^String url]
  (if (blank? url)
    nil
    (.toString (url-like url))))

(defn valid? [^String url]
  (and (not (blank? url)) (> 2000 (.length url)) (> (.indexOf url ".") 0)))

(defn set-config!
  ([] (set-config! "squink.conf.edn"))
  ([filename]
   (let [file-data (edn/read-string (slurp filename))
         full-data (merge *config* file-data)]
     (set! *config* (dissoc full-data :db))
     (set! *db* (:db full-data))
     true)))