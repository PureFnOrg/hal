(ns hal.core
  (:import
    [javax.crypto Cipher KeyGenerator]
    [javax.crypto.spec SecretKeySpec IvParameterSpec]
    [java.security SecureRandom]
    [java.util Base64]))


;; util fns

(def byte-array-class (Class/forName "[B"))

(defn byte-array?
  "Tests if a value is a byte[]. Returns true/false
  object - the object to test"
  [object]
  (= byte-array-class (type object)))

(defn rand-16-byte-array
  "Returns a byte[] of 16 random byes"
  []
  (let [sr    (SecureRandom/getInstance "SHA1PRNG")
        bytes (byte-array 16)]
    (.nextBytes sr bytes)
    bytes))

(defn raw-aes-key
  "Returns an AES SecretKey encoded as a byte[]
  seed - (String)the seed to initialize the key with"
  [^String seed]
  (let [keygen (KeyGenerator/getInstance "AES")
        sr     (SecureRandom/getInstance "SHA1PRNG")]
    (.setSeed ^SecureRandom sr (.getBytes seed "UTF-8"))
    (.init keygen 128 sr)
    (.getEncoded (.generateKey keygen))))

(defn cipher
  "Returns an AES/CBC/PKCS5Padding Cipher which can be used to encrypt or decrypt a
  byte[], depending on the mode of the Cipher.
  mode     - (int) see https://docs.oracle.com/javase/7/docs/api/javax/crypto/Cipher.html
                   for available modes. Typically this will be either Cipher/ENCRYPT_MODE
                   or Cipher/DECRYPT_MODE.
  seed     - (String) the encryption seed / secret
  iv-bytes - (byte[]) the initialization vector for the cipher"
  ^Cipher
  [^Integer mode ^String seed ^bytes iv-bytes]
  (let [key-spec (SecretKeySpec. (raw-aes-key seed) "AES")
        iv-spec  (IvParameterSpec. iv-bytes)
        cipher   (Cipher/getInstance "AES/CBC/PKCS5Padding")]
    (.init cipher (int mode) key-spec iv-spec)
    cipher))


;; encryption

(defn encrypt
  "Symmetrically encrypts value with encryption-key, such that it can be
  decrypted later with (decrypt).
  Returns byte[]. The first 16 bytes of the returned value are the
  initialization vector. The remainder is the encrypted data.
  value           - (String/byte[]) the value to encrypt. Throws IllegalArgumentException
                    if value is an invalid type. Note: Assumes String is UTF-8!
  encryption-key  - (String) the key with which to encrypt value with. Throws
                    IllegalArgumentException if encryption-key is empty."
  ^bytes
  [value encryption-key]
  (when (not (or (string? value)
               (byte-array? value)))
    (throw (IllegalArgumentException. "Argument [value] must be of type String or byte[]")))

  (when (empty? encryption-key)
    (throw (IllegalArgumentException. "Argument [encryption-key] must not be empty")))

  (let [value-bytes (if (string? value) (.getBytes ^String value "UTF-8") ^bytes value)
        iv-bytes    (rand-16-byte-array)
        cipher      (cipher Cipher/ENCRYPT_MODE encryption-key iv-bytes)]
    (into-array Byte/TYPE (concat iv-bytes
                            (.doFinal cipher value-bytes)))))

(defn decrypt
  "Decrypts a value which has been encrypted via (encrypt). Returns byte[].
  The first 16 bytes of the input value is the initialization vector to use when
  decrypting. The remainder is the encrypted data.
  value          - (byte[]) the value to decrypt
  encryption-key - (String) the key with which the value was encrypted"
  ^bytes
  [value encryption-key]
  (let [[iv-bytes encrypted-data] (split-at 16 value)
        iv-bytes       (into-array Byte/TYPE iv-bytes)
        encrypted-data (into-array Byte/TYPE encrypted-data)
        cipher         (cipher Cipher/DECRYPT_MODE encryption-key iv-bytes)]
    (.doFinal cipher encrypted-data)))

(defn decrypt-as-str
  "Decrypts a value which has been encrypted via (encrypt) and attempts to
  construct the decrypted value into a string.
  value              - (byte[]) the value to decrypt
  encryption-key     - (String) the key with which the value was encrypted"
  [value encryption-key]
  (String. (decrypt value encryption-key) "UTF-8"))

;; base-64 string fns
(defn encrypt-as-base64
  "Symmetrically encrypts value with encryption-key, returning a base64 encoded string, such that it can be
  decrypted later with (decrypt-from-base64).
  value              - (String/byte[]) the value to encrypt.
  encryption-key     - (String) the key with which to encrypt value with."
  [value encryption-key]
  (.encodeToString (Base64/getEncoder) (encrypt value encryption-key)))

(defn decrypt-from-base64
  "Decrypts a value which has been encrypted via (encrypt-as-base64) and attempts to
  construct the decrypted value into a string.
  value              - (String) the value to decrypt encoded as a base64 string
  encryption-key     - (String) the key with which the value was encrypted"
  [value encryption-key]
  (decrypt-as-str (.decode (Base64/getDecoder) (.getBytes ^String value)) encryption-key))




