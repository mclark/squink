(ns squink.hash-test
  (:require [midje.sweet :refer :all]
            [squink.hash :refer :all]))

(fact "base62 converts BigIntegers appropriately"
      (base62 (BigInteger. "55")) => '(\t)
      (base62 (BigInteger. "63")) => '(\1 \1))

(fact "murmur3-128 hashes to the same big number every time"
      (murmur3-128 "test-string") => 251929014245022739636775903609045822017
      (murmur3-128 "hello") => (partial instance? BigInteger))

(fact "murmur3-32 hashes to the same smaller number every time"
      (murmur3-32 "test-string") => 3239198007
      (murmur3-128 "hello") => (partial instance? BigInteger))

(fact "hash128 hashes to the same base62 string every time"
      (hash128 "something") => "6AnpRXLiVIfg93A6HRrmVF"
      (hash128 nil) => nil)

(fact "hash32 hashes to the same base62 string every time"
      (hash32 "something") => "32pFKj"
      (hash32 nil) => nil)