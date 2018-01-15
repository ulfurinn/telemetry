(defproject net.ulfurinn/telemetry "0.1.0"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [
  	[org.clojure/clojure "1.8.0"]
  	[alaisi/postgres.async "0.8.0"]
  	[org.postgresql/postgresql "42.1.4"]
  	[migratus "1.0.1"]
  	[cheshire "5.5.0"]
  ]
  :plugins [[migratus-lein "0.5.3"]])
