(ns hal.token
  (:require
    [clojure.spec.alpha :as s]
    [clojure.spec.gen.alpha :as gen]
    [clojure.edn :as edn]
    [hal.core :as h])
  (:import
    [java.util Date Map]
    [org.joda.time DateTime])
  (:gen-class
    :name org.purefn.Token
    :methods [#^{:static true} [mint [java.util.Map] String]
              #^{:static true} [mint [java.util.Map String] String]
              #^{:static true} [decrypt [String] java.util.Map]
              #^{:static true} [decrypt [String String] java.util.Map]]))

(defn- valid-input?
  [o]
  (or (number? o)
    (string? o)
    (instance? DateTime o)
    (instance? Date o)))

(defn- secret
  []
  (or 
   ;; todo - this should be user set/initialized.  at Ladders it was a lazily evaluated
   ;; call into secret manager code.
   "foo"))

;; Token encryption - Clojure API
(defn mint
  "Returns a token-string."
  ([m]
   (mint m (secret)))
  ([m secret-key]

   (assert (contains? (supers (class m)) Map)
           "Token data must be represented as a Map.")

   (assert (every? string? (keys m))
           "Keys must be Strings.")

   (assert (every? valid-input? (vals m))
           "Values must be `String`s, `number`s, `java.util.Date`s, or `org.joda.time.DateTime`s.")

   (h/encrypt-as-base64 (pr-str m) secret-key)))

(defn decrypt
  "Decrypts a token-string to a clojure Map."
  ([^String s]
   (decrypt s (secret)))
  ([^String s ^String secret-key]
   (edn/read-string (h/decrypt-from-base64 ^String s ^String secret-key))))

;; Token encryption - Java API
(defn -mint
  "Java wrapper around the `mint` fn."
  ([m]
   (mint m (secret)))
  ([m secret-key]
   (mint m secret-key)))

(defn -decrypt
  "Java wrapper around the `decrypt` fn."
  ([s]
   (decrypt s (secret)))
  ([s secret-key]
   (decrypt s secret-key)))


;; Specs

(defn token?
  "Returns whether given string is base64 encrypted token"
  [s]
  (try
    (h/decrypt-from-base64 ^String s (secret))
    true
    (catch Exception e false)))

(s/def ::date (s/with-gen
                (partial instance? Date)
                #(gen/return (Date.))))
(s/def ::datetime (s/with-gen
                    (partial instance? DateTime)
                    #(gen/return (DateTime.))))
(s/def ::token (s/with-gen
                 token?
                 #(gen/return (mint (gen/generate (s/gen ::token-data))))))
(s/def ::token-data-val (s/or :number number?
                              :string string?
                              :datetime ::datetime
                              :date ::date))
(s/def ::token-data (s/every-kv string? ::token-data-val :into {}))

(s/fdef mint
  :args (s/cat :token-data ::token-data)
  :ret ::token)

(s/fdef decrypt
  :args (s/cat :token-string ::token)
  :ret ::token-data)
