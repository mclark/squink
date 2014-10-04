(ns squink.squink.hash
  (:import com.google.common.hash.Hashing
           clojure.lang.Murmur3))

(def alphabet62 (vec (map char (concat (range 48 58) (range 65 91) (range 97 123)))))

(def base (BigInteger. "62"))

(defn base62 [^BigInteger n]
  (loop [orig n converted ""]
    (let [chars (cons (nth alphabet62 (mod orig base)) converted)]
      (if (>= orig base) (recur (.divide orig base) chars) chars))))

(defn murmur3-128 [url]
  (let [h (Hashing/murmur3_128)
        bytes (.. h (hashString url) (asBytes))]
    (BigInteger. 1 bytes)))

(defn murmur3-32 [url]
  (let [h (Hashing/murmur3_32)
        bytes (.. h (hashString url) (asBytes))]
    (BigInteger. 1 bytes)))

(defn hash32 [url]
  (apply str (base62 (murmur3-32 url))))

(defn hash128 [url]
  (apply str (base62 (murmur3-128 url))))