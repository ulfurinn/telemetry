(ns telemetry.db
  (:use [clojure.tools.logging :only [info error]]
        [riemann.common :only [custom-attributes]]
        [riemann.streams :only [part-time-simple]])
  (:require [migratus.core :as migratus]
            [postgres.async :as pg]
            postgres.async.json))

(defn- prop [env propname default]
  (or (get (System/getenv) env)
      (System/getProperty propname)
      default))

(defn host [] (prop "TLM_DB_HOST" "telemetry.db.host" "localhost"))
(defn dbname [] (prop "TLM_DB_NAME" "telemetry.db.name" "telemetry"))
(defn user [] (prop "TLM_DB_USER" "telemetry.db.user" "telemetry"))
(defn pass [] (prop "TLM_DB_PASS" "telemetry.db.pass" "telemetry"))

(defn- init []
  (let [config {:store :database
                :migration-dir "migrations/"
                :migration-table-name "schema_migrations"
                :db {:classname "org.postgresql.Driver"
                     :subprotocol "postgresql"
                     :subname (str "//" (host) "/" (dbname))
                     :user (user)
                     :password (pass)}}]
    (migratus/migrate config)))

(defn connect []
  (init)
  (pg/open-db {:hostname (host)
               :database (dbname)
               :username (user)
               :password (pass)}))

(defn- long-str [& strings]
  (clojure.string/join " " strings))

(defn- insert-query [table]
  (long-str (str "INSERT INTO " table " (service, host, metric, state, description, ttl, timestamp, tags, attributes)")
            "VALUES ($1, $2, $3, $4, $5, $6, to_timestamp($7), ARRAY(SELECT jsonb_array_elements_text($8)), $9)"))

(defn- upsert-query [table]
  (long-str (str "INSERT INTO " table " (service, host, metric, state, description, ttl, timestamp, tags, attributes)")
            "VALUES ($1, $2, $3, $4, $5, $6, to_timestamp($7), ARRAY(SELECT jsonb_array_elements_text($8)), $9)"
            "ON CONFLICT (service, host) DO"
            "UPDATE SET metric = EXCLUDED.metric, state = EXCLUDED.state, description = EXCLUDED.description, ttl = EXCLUDED.ttl, timestamp = EXCLUDED.timestamp, tags = EXCLUDED.tags, attributes = EXCLUDED.attributes"))

(defn- dbstream [dbconn query]
  (let [to-float (fn [x] (if x (double x) nil))]
    (fn stream [event]
      (pg/execute! dbconn [query
                           (:service event)
                           (:host event)
                           (to-float (:metric event))
                           (:state event)
                           (:description event)
                           (:ttl event)
                           (to-float (:time event))
                           (:tags event)
                           (custom-attributes event)] (fn [ok err] (if err (error [event err])))))))

(defn insert [dbconn table]
  (dbstream dbconn (insert-query table)))

(defn upsert [dbconn table]
  (dbstream dbconn (upsert-query table)))

(defn index [dbconn] (upsert dbconn "riemann_index"))

(def insert-inventory-query (long-str "INSERT INTO riemann_inventory (service, host, description, tags, last_seen)"
                                      "VALUES ($1, $2, $3, ARRAY(SELECT jsonb_array_elements_text($4)), to_timestamp($5))"
                                      "ON CONFLICT (service, host) DO"
                                      "UPDATE SET description = EXCLUDED.description, last_seen = EXCLUDED.last_seen, tags = EXCLUDED.tags"))

(defn inventory [dbconn]
  (let [to-float (fn [x] (if x (double x) nil))
        flush-one (fn [[_ event]] (pg/execute! dbconn [insert-inventory-query (:service event) (:host event) (:description event) (:tags event) (to-float (:time event))] (fn [ok err] (if err (error [event err])))))
        flush-all (fn [collection] (doall (map flush-one collection)))]
    (part-time-simple 60
                      (fn [_] (hash-map))
                      (fn [state event] (assoc state [(:service event) (:host event)] event))
                      (fn [collection _ _] (flush-all collection)))))
