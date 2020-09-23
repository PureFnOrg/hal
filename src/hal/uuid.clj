(ns hal.uuid
  "Contains functions for the bi-directional conversion of an interger to a type 6 UUID.

  This helps to overcome one of the challenges of transitioning from a system with
  monotonically increasing ids (SQL) to a distributed one.  Defining and using a new
  type of UUID allows us to

  1) Encode the original serial id in the guid,

  2) creates an easy transtion to continue to use UUIDs once serial ids are no longer an
     authority on identifying data and,

  3) Allows for serial id based UUIDs to be easily human and machine identifiable

  A type 6 UUID represents the originating SQL table in octets 0-3, the integer in
  octets 4-7, octet 8 as the variant, and all other octets as 0.

  For example the integer 255 from the SQL table recruiter_profile will be represented
  as:

  00000001-0000-00ff-c000-000000000000"
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen])
  (:import [java.util UUID])
  (:refer-clojure :exclude [int])
  (:gen-class
   :name org.purefn.Type6UUID
   :methods [#^{:static true} [type6 [String Integer] java.util.UUID]
             #^{:static true} [id [java.util.UUID] Integer]
             #^{:static true} [table [java.util.UUID] String]]))

(def tables
  [["my_table"  0x1]
   ["my_other_table" 0x2]])

(def table->id
  (into {} tables))

(def id->table
  (into {} (map (comp vec reverse)) tables))

(def lsbs-with-variant
 "Octet 8 of the most significant bits of a UUID is reserved for the variant,
  or type, of the UUID.  This produces the 64 bit word

  0xc000000000000000

  which specifies this is a type 6 UUID (not part any official spec) with the
  rest of the word zeroed out."
  (bit-shift-left 0x3 62))

(defn type-6
  "Produces a type 6 UUID from a SQL table name and integer."
  [table-name i]
  (when-not (and (pos? i) (<= i Integer/MAX_VALUE))
    (throw (ex-info (format "%s is out of range!" i)
                    {:integer i
                     :table-name table-name})))
  (if-let [table-id (table->id table-name)]
    (UUID. (bit-or (bit-shift-left table-id 32)
                   (clojure.core/int i))
           lsbs-with-variant)
    (throw (ex-info (format "%s not found in table mappings" table-name)
                    {:table-name table-name
                     :table-mappings table->id}))))

(defn type6?
  "True if the uuid is type6, false otherwise."
  [uuid]
  (= (.variant uuid) 6))

(defprotocol UUIDType6
  (int [uuid]
    "The integer component encoded into the type 6 UUID, or nil if not type 6.")
  (table [uuid]
    "The sql table name component encoding into the type 6 UUID, or nil if not type 6.")
  (sql-id [uuid]
    "A tuple of [sql-table integer] encoded into the type 6 UUID, or nil if not type 6."))

(extend-protocol UUIDType6
  UUID
  (int [uuid]
    (when (type6? uuid)
      (-> (.getMostSignificantBits uuid)
          (bit-and 0x00000000FFFFFFFF))))

  (table [uuid]
    (when (type6? uuid)
      (-> (.getMostSignificantBits uuid)
          (bit-shift-right 32)
          (id->table))))

  (sql-id [uuid]
    (when (type6? uuid)
      ((juxt table int) uuid)))

  String
  (int [uuid] (int (UUID/fromString uuid)))
  (table [uuid] (table (UUID/fromString uuid)))
  (sql-id [uuid] (sql-id (UUID/fromString uuid))))

(defn sql-table-guid?
  "Predicate for validating if a guid was generated from a certain table."
  [table-name uuid]
  (when uuid
    (= table-name (table uuid))))

(defmacro defspec
  "Create a spec for a sql-generated guid of the name `spec-id` for the sql table
  `table-name`.

  If an optional paramter `:non-type6?` is truthy, will allow non type6 UUIDs to pass
  spec.

  e.g. (uuid/defspec ::id \"my_table\")
       (uuid/defspec ::id \"my_table\" :non-type6? true)"
  [spec-id table-name & {:keys [non-type6?]}]
  `(s/def ~spec-id
     (s/with-gen
       ~(if non-type6?
          `#(if ((every-pred uuid? type6?) %)
              (sql-table-guid? ~table-name %)
              (uuid? %))
          `(s/and uuid? (partial sql-table-guid? ~table-name)))
       #(gen/fmap (partial type-6 ~table-name)
                  (s/gen (s/int-in 1 Integer/MAX_VALUE))))))

;;------------------------------------------------------------
;; Java API
;;------------------------------------------------------------

(defn -type6
  "Create a type 6 UUID from a sql table name and integer PK."
  [table-name sql-id]
  (type-6 table-name sql-id))

(defn -table
  "Retrieve the table name encoded in the type 6 UUID."
  [uuid]
  (first (sql-id uuid)))

(defn -id
  "Retrieve the PK integer encoded in the type 6 UUID."
  [uuid]
  (-> (sql-id uuid)
      (second)
      (clojure.core/int)))
