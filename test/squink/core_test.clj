(ns squink.core-test
  (:require [midje.sweet :refer :all]
            [squink.core :refer :all]
            [squink.mysql :as db]
            [squink.hash :as h]))

(facts "set-config!"
       (fact "it uses squink.conf.edn by default"
             (binding [*config* {} *db* {}]
               (set-config!) => true
               (provided (slurp "squink.conf.edn") => "{}")))
       (fact "it sets the *config* and *db* vars by merging in their original values"
             (binding [*config* {:test "hello"}
                       *db* {:subprotocol "mysql"}]
               (set-config!) => true
               (provided (slurp "squink.conf.edn") => "{:db {:a \"b\"} :foo \"bar\"}")
               (= *config* {:foo "bar" :test "hello"}) => true
               (= *db* {:a "b"}) => true))

       (fact "it slurps from the provided file"
             (binding [*config* {} *db* {}]
               (set-config! "hello.conf.edn") => true
               (provided (slurp "hello.conf.edn") => "{}"))))

(facts "valid?"
       (valid? nil) => false
       (valid? "") => false?
       (valid? "test") => true
       (valid? (apply str (repeat 300 "abcdefg"))) => false)

(facts "sanitize-url"
       (sanitize-url "http://www.test.com") => "http://www.test.com/"
       ;(sanitize-url "www.test.com") => "http://www.test.com"
       ;(sanitize-url "test.com") => "http://test.com"
       )

(let [url "http://www.varagesale.com"
      hashed (h/hash128 url)
      default-stem (*config* :base-stem)
      prefix (fn [len] (apply str (take len hashed)))
      default-prefix "l4"]                                  ; TODO workaround as `provided` arguments cannot be the results of function calls.

  (facts "shorten"
         (against-background (db/insert-url *db* url anything) => url
                             (db/lookup-url *db* anything) => nil)

         (fact "will attempt to shorten based on configured base stem"
               (with-redefs [*config* (merge *config* {:base-stem 5})]
                 (shorten url) => (prefix 5)))

         (fact "will add next hash character if there is a conflict"
               (shorten url) => (prefix (inc default-stem))
               (provided (db/lookup-url *db* default-prefix) => "http://www.youtube.com"))

         (facts "when the entire hash conflicts"
                (against-background (db/lookup-url *db* anything) => "google.ca")

                (fact "will prepend 1+ to url and use that hash"
                      (shorten url) => (->> (str "1+" url) h/hash128 (take 2) (apply str))
                      (provided (db/lookup-url *db* "u8") => nil))

                (fact "will return nil if no unique hash exists"
                      (shorten url) => nil)

                (fact "will only retry the configured times"
                      (with-redefs [*config* (merge *config* {:retry-count 0})]
                        (shorten url) => nil
                        (provided (db/lookup-url *db* "u8") => nil :times 0))))))
