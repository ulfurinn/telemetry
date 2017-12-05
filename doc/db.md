# `telemetry.db`

The `telemetry.db` module provides streams for storing Riemann events in PostgreSQL.

Postgres >= 9.5 is required.

## Functions

### `(connect)`

This function returns a DB connection object. Migrations will be run as necessary. Call it once and reuse the returned value between streams.

You can use this value in custom queries with `postgres.async/execute!` and friends.

### `(index dbconn)`

This roughly corresponds to the built-in Riemann index.

Events passed to this stream will be recorded in the `riemann_index` table. Every event will issue a query, so avoid using this with high-rate services.

If the state of a service changes between two successive events, a record will be appended to the table `riemann_index_state_log`.

### `(inventory dbconn)`

Events passed to this stream will be recorded in the `riemann_inventory` table.

## `index` vs `inventory`

These two streams are similar but serve different purposes and should be used with different inputs.

The purpose of `index` is to gather a real-time snapshot of the system state, and works best with low-rate services with occasional flapping (once per second or less) that provide a state. Think "health probe" or "free memory" rather than "http request" or "method call". A high-rate service with a lot of flapping, like a frequently failing method call, will strain the DB with updates and quickly bloat the state log table.

The purpose of `inventory` is to provide a complete list of every existing service for documentation purposes. It can be an immediate child of `(streams)` with no filtering. It uses a long buffer internally, so high-rate services will not overload it.