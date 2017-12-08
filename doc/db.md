# `telemetry.db`

The `telemetry.db` module provides streams for storing Riemann events in PostgreSQL.

Postgres >= 9.5 is required.

## Connection parameters

The following are evaluated left to right, ending at the first one that yields a non-nil value.

Custom Java properties can be passed to Riemann through `$EXTRA_JAVA_OPTS` set in `/etc/default/riemann` (or equivalent).

| Environment variable | Java property | Default |
| --- | --- | --- |
| `TLM_DB_HOST` | `telemetry.db.host` | `localhost` |
| `TLM_DB_NAME` | `telemetry.db.name` | `telemetry` |
| `TLM_DB_USER` | `telemetry.db.user` | `telemetry` |
| `TLM_DB_PASS` | `telemetry.db.pass` | `telemetry` |

## Functions

### `(connect)`

This function returns a DB connection object. Migrations will be run as necessary. Call it once and reuse the returned value between streams.

You can use this value in custom queries with `postgres.async/execute!` and friends.

### `(index dbconn)`

This roughly corresponds to the built-in Riemann index.

Events passed to this stream will be recorded in the `riemann_index` table. Every event will issue a query, so avoid using this with high-rate services.

Tags are inserted into a `text[]` column. Customer attributes are inserted into a `jsonb` column. Keep in mind that all custom attributes in Riemann are initially decoded into strings; pre-process the events as necessary.

If the state of a service changes between two successive events with the same service and host, a record will be appended to the table `riemann_index_state_log`. This is managed by a table trigger.

### `(insert dbconn table)`, `(upsert dbconn table)`

Generalizations of `(index)` for use with custom tables. `(index)` itself is defined as `(upsert dbconn "riemann_index")`.

The required minimal definition to use with upserts is:

```sql
CREATE TABLE table_name (
	service text NOT NULL,
	host text,
	metric double precision,
	state varchar(255),
	description text,
	ttl double precision,
	timestamp timestamp,
	tags text[],
	attributes jsonb,
	PRIMARY KEY (service, host)
);
```

For inserts, put the primary key on an `id serial` column.

### `(inventory dbconn)`

Events passed to this stream will be recorded in the `riemann_inventory` table.

## `index` vs `inventory`

These two streams are similar but serve different purposes and should be used with different inputs.

The purpose of `index` is to gather a real-time snapshot of the system state, and works best with low-rate services with occasional flapping (once per second or less) that provide a state. Think "health probe" or "free memory" rather than "http request" or "method call". A high-rate service with a lot of flapping, like a frequently failing method call, will strain the DB with updates and quickly bloat the state log table.

The purpose of `inventory` is to provide a complete list of every existing service for documentation purposes, as in can be tricky to find which services are provided by any given part of a large system. It can be an immediate child of `(streams)` with no filtering. It uses a long buffer internally, so high-rate services will not overload it.
