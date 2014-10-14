(ns squink.mysql-test
  (:require [midje.sweet :refer :all]
            [clojure.java.jdbc :refer [with-db-transaction db-set-rollback-only! insert!]]
            [squink.mysql :refer :all]))

(def test-db {:subprotocol "mysql"
         :subname     "//192.168.59.103:3306/squink_test"
         :user        "root"})

(declare ^{:private true :dynamic true} conn)

(with-state-changes
  [(before :facts (create-schema test-db true))
   (around :facts (with-db-transaction [c test-db]
                                       (db-set-rollback-only! c)
                                       (binding [conn c] ?form)))]
  (facts "find-url"
         (fact "unrecognized hashes should return nil"
               (find-url conn "unknown-hash") => nil)

         (fact "with a previously inserted url"
               (let [url "http://facebook.com"
                     hash "somehash"]
                 (insert-url conn url hash)
                 (find-url conn hash) => url)))

  (facts "lookup-url"
         (with-state-changes
           [(before :facts (reset! memoized {}))]
           (fact "with an unknown hash returns nil"
                 (lookup-url conn "foobar") => nil
                 (provided (find-url conn "foobar") => nil))

           (fact "subsequent hits use cached value"
                 (lookup-url conn "foobar2") => "mysite"
                 (provided (find-url conn "foobar2") => "mysite")

                 (lookup-url conn "foobar2") => "mysite"
                 (provided (find-url anything anything) => :irrelevant :times 0))

           (fact "cache is updated after insert"
                 (lookup-url conn "hi") => nil
                 (insert-url conn "http://hello.com" "hi")
                 (lookup-url conn "hi") => "http://hello.com")))

  (facts "insert-url"
         (fact "the url and hash is not present in the database"
               (fact "it should be returned"
                     (let [url "http://www.cnn.com"]
                       (insert-url conn url "sffDf") => url)))

         (fact "the url already exists undert the same hash"
               (fact "it should be returned"
                     (let [url "http://www.facebook.com" hash "abc123"]
                          (insert! conn :shortened_url
                                   {:url url :hashed hash})
                          (insert-url conn url hash) => url)))

         (fact "the hash is already used by a different url"
               (fact "it should return nil"
                     (let [hashed "abc123"]
                          (insert! conn :shortened_url
                                   {:url "http://www.fresh.com" :hashed hashed})
                          (insert-url conn "http://unique.com" hashed) => nil)))))
