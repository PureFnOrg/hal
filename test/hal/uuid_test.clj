(ns hal.uuid-test
  (:refer-clojure :exclude [int])
  (:require [hal.uuid :refer :all]
            [clojure.test :refer [deftest is]]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen])
  (:import [java.util UUID]))

(def num-values 1000)

(s/def ::sql-int (s/and pos-int? #(<= % Integer/MAX_VALUE)))

(deftest conversion
  (let [random-uuid (UUID/randomUUID)
        table-name "my_table"]
    (doseq [i (cons Integer/MAX_VALUE (gen/sample (s/gen ::sql-int) num-values))]
      (let [uuid (type-6 table-name i)]
        (is (= uuid (type-6 table-name i)))
        (is (= i (int uuid)))
        (is (= table-name (table uuid)))
        (is (= [table-name i] (sql-id uuid)))
        (is (not= uuid random-uuid))))))

(deftest non-type6
  (let [id #uuid "00000002-058c-8aee-c000-000000000000"
        other-id #uuid "00000003-058c-8aee-c000-000000000000"]
    (defspec ::type6-id "my_other_table")
    (defspec ::type6-ish-id "my_other_table" :non-type6? true)

    (is (not (s/valid? ::type6-id (UUID/randomUUID))))
    (is (not (s/valid? ::type6-id other-id)))
    (is (s/valid? ::type6-id id))
    (is (not (s/valid? ::type6-ish-id (str id))))
    
    (is (s/valid? ::type6-ish-id id))
    (is (not (s/valid? ::type6-ish-id other-id)))
    (is (s/valid? ::type6-ish-id (UUID/randomUUID)))
    (is (not (s/valid? ::type6-ish-id (str id))))))
