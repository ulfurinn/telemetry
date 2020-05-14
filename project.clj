(defproject net.ulfurinn/telemetry "0.1.8-SNAPSHOT"
  :description "Some helpful Riemann streams."
  :url "https://github.com/ulfurinn/telemetry"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [alaisi/postgres.async "0.8.0"]
                 [org.postgresql/postgresql "42.2.12"]
                 [migratus "1.2.8"]
                 [cheshire "5.5.0"]]
  :plugins [[migratus-lein "0.5.3"]
            [lein-pprint "1.2.0"]])
