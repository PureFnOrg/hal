(ns hal.core-test
  (:require [org.purefn.kurosawa.config]
            [clojure.test :refer :all]
            [hal.core :refer :all]
            [hal.token])
  (:import [java.util HashMap]
           [javax.crypto Cipher BadPaddingException]
           [java.security InvalidAlgorithmParameterException]))

(deftest byte-array?-test
  (is (byte-array? (byte-array 10)))
  (is (not (byte-array? nil)))
  (is (not (byte-array? (seq "hey")))))

(deftest rand-16-bytes-test
  (let [iv1 (rand-16-byte-array)
        iv2 (rand-16-byte-array)]
    (is (byte-array? iv1))
    (is (byte-array? iv2))
    (is (not (empty? iv1)))
    (is (not (empty? iv2)))
    (is (not= (seq iv1) (seq iv2)))))

(deftest raw-aes-key-test
  (let [raw-aes-key (raw-aes-key "secret")]
    (is (byte-array? raw-aes-key))
    (is (not (empty? raw-aes-key))))
  (is (thrown? ClassCastException (raw-aes-key :invalid))))

(deftest cipher-test
  (let [iv-bytes (rand-16-byte-array)]
    (let [cipher (cipher Cipher/ENCRYPT_MODE "secret" iv-bytes)]
      (is (instance? Cipher cipher))
      (is (= (seq iv-bytes) (seq (.getIV cipher)))))
    (is (thrown? ClassCastException (cipher "mode" "secret" iv-bytes)))
    (is (thrown? ClassCastException (cipher Cipher/ENCRYPT_MODE :invalid iv-bytes)))
    (is (thrown? ClassCastException (cipher Cipher/ENCRYPT_MODE "secret" :invalid)))
    (is (thrown? InvalidAlgorithmParameterException (cipher Cipher/ENCRYPT_MODE "secret" (byte-array 3))))))


(def secret "test key")
(def value "test value")

(deftest test-encrypt
  (testing "Testing `encrypt` fn."
    (is (not= value (String. (encrypt value secret))))
    (is (byte-array? (encrypt value secret)))
    (is (thrown? IllegalArgumentException (encrypt :invalid secret)))
    (is (thrown? IllegalArgumentException (encrypt value :invalid)))
    (is (thrown? IllegalArgumentException (encrypt value "")))))

(deftest test-decrypt
  (testing "Testing `decrypt` fn."
    (is (= value (String. (decrypt (encrypt value secret) secret))))
    (is (byte-array? (decrypt (encrypt value secret) secret)))
    (is (thrown? IllegalArgumentException (decrypt :invalid secret)))
    (is (thrown? ClassCastException (decrypt (encrypt value secret) :invalid)))))

(deftest test-decrypt-as-str
  (testing "Testing `decrypt-as-str` fn."
    (is (= value (decrypt-as-str (encrypt value secret) secret)))
    (is (thrown? IllegalArgumentException (decrypt-as-str :invalid secret)))
    (is (thrown? ClassCastException (decrypt-as-str (encrypt value secret) :invalid)))))

(deftest test-encrypt-as-base64
  (testing "Testing `encrypt-as-base64` fn."
    (is (not= value (encrypt-as-base64 value secret)))
    (is (string? (encrypt-as-base64 value secret)))
    (is (thrown? IllegalArgumentException (encrypt-as-base64 :invalid secret)))
    (is (thrown? IllegalArgumentException (encrypt-as-base64 value :invalid)))
    (is (thrown? IllegalArgumentException (encrypt-as-base64 value "")))))

(deftest test-decrypt-from-base64
  (testing "Testing `decrypt-from-base64` fn."
    (is (= value (decrypt-from-base64 (encrypt-as-base64 value secret) secret)))
    (is (thrown? ClassCastException (decrypt-from-base64 :invalid secret)))
    (is (thrown? ClassCastException (decrypt-from-base64 (encrypt-as-base64 value secret) :invalid)))))


(def valid-clj-data {"test key" "test value"})
(def valid-java-data (let [m (new HashMap)]
                       (doto m (.put "1" 2)
                               (.put "a" 3))
                       m))

(def invalid-clj-data {:testkey "test value"})
(def invalid-java-data (let [m (new HashMap)]
                         (doto m (.put 1 2)
                                 (.put "a" 3))
                         m))

(deftest test-token-mint
  (testing "Testing `hal.token/mint` fn"
    ;; clj
    (is (not= valid-clj-data (hal.token/mint valid-clj-data)))
    (is (string? (hal.token/mint valid-clj-data)))
    (is (thrown? AssertionError (hal.token/mint invalid-clj-data)))
    ;; java
    (is (not= valid-java-data (hal.token/mint valid-java-data)))
    (is (string? (hal.token/mint valid-java-data)))
    (is (thrown? AssertionError (hal.token/mint invalid-java-data)))
    ;; bad-arg
    (is (thrown? AssertionError (hal.token/mint :invalid)))))

(deftest test-token-decrypt
  (testing "Testing `hal.token/decrypt` fn."
    (is (= valid-clj-data (hal.token/decrypt (hal.token/mint valid-clj-data))))
    (is (thrown? ClassCastException (hal.token/decrypt :invalid)))
    (is (thrown? BadPaddingException (hal.token/decrypt (encrypt-as-base64 "{}" secret))))
    (is (= valid-java-data (hal.token/decrypt (hal.token/mint valid-java-data))))))
