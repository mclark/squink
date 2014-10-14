;(ns squink.core
;  (:require
;            [clojure.string :refer [blank? trim]]
;            [clojure.edn :as edn]
;            [ring.middleware.params :refer [wrap-params]]
;            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
;            [compojure.core :refer [defroutes GET POST]]
;            [clojurewerkz.urly.core :refer [url-like]]
;            [ring.adapter.jetty :refer [run-jetty]]
;            [squink.hash :refer [hash128]])
;  (
;           java.io.PushbackReader)
;  (:gen-class))
;
;(def config {:retry-count 3 :base-stem 2})
;
;(def memoized (atom {}))
;
;(defn lookup-url [code]
;  (if-let [url (get @memoized code)]
;    url
;    (if-let [url (find-url db code)]
;      (get (swap! memoized
;                  (fn [m] (if-not (get m code) (assoc m code url) m)))
;           code))))
;
;(defn persist [url hashed]
;  (loop [char-count (:base-stem config)]
;    (let [h (apply str (take char-count hashed))
;          existing (or (lookup-url h) (insert-url db url h))]
;      (if (= existing url)
;        h
;        (if (< char-count (count hashed))
;          (recur (inc char-count))
;          nil)))))
;
;(defn shorten [url]
;  (loop [candidate-url url tries-remaining (:retry-count config)]
;    (let [hashed (hash128 candidate-url)
;          persisted-hash (persist candidate-url hashed)]
;      (if (and (nil? persisted-hash)
;               (< 0 tries-remaining))
;        (recur (str tries-remaining "+" url) (dec tries-remaining))
;        persisted-hash))))
;
;(defn- sanitize-url [^String url]
;  (if (blank? url)
;    nil
;    (.toString (url-like url))))
;
;(defn- valid? [^String url]
;  (and (not (blank? url)) (> 2000 (.length url))))
;
;(defn read-config []
;  (def config (edn/read-string (slurp "squink.conf.edn")))
;  (def db (:db config)))