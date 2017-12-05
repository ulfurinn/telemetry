# telemetry

A metrics and monitoring framework built on Riemann.

Using the `telemetry.db` module requires PostgreSQL >= 9.5.

## Usage

Run Riemann with `telemetry.jar` included in `$EXTRA_CLASSPATH`.

See `riemann.config.example` and `doc/` for more details.

### Connection parameters

The following are evaluated left to right, ending at the first one that yields a non-nil value.

Custom Java properties can be passed to Riemann through `$EXTRA_JAVA_OPTS` set in `/etc/default/riemann` (or equivalent).

| Environment variable | Java property | Default |
| --- | --- | --- |
| TLM_DB_HOST | telemetry.db.host | localhost |
| TLM_DB_NAME | telemetry.db.name | telemetry |
| TLM_DB_USER | telemetry.db.user | telemetry |
| TLM_DB_PASS | telemetry.db.pass | telemetry |

## License

Copyright © 2017 Valeri Sokolov

Distributed under the Eclipse Public License version 1.0.
