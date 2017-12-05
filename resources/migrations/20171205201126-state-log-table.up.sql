CREATE TABLE riemann_index_state_log (
	id bigserial,
	service text NOT NULL,
	host text,
	metric double precision,
	state varchar(255),
	description text,
	timestamp timestamp,
	PRIMARY KEY (id)
);

CREATE INDEX ON riemann_index_state_log (service);
CREATE INDEX ON riemann_index_state_log (host);

CREATE OR REPLACE FUNCTION fn_insert_state_log_change() RETURNS trigger AS
$$
BEGIN
	IF TG_OP = 'INSERT' OR (TG_OP = 'UPDATE' AND NEW.state IS DISTINCT FROM OLD.state) THEN
		INSERT INTO riemann_index_state_log
		(service, host, metric, state, description, timestamp)
		VALUES
		(NEW.service, NEW.host, NEW.metric, NEW.state, NEW.description, NEW.timestamp);
	END IF;

	RETURN NEW;
END;
$$
LANGUAGE 'plpgsql';

CREATE TRIGGER trg_insert_state_log_change
AFTER INSERT OR UPDATE OF state
ON riemann_index
FOR EACH ROW
EXECUTE PROCEDURE fn_insert_state_log_change();
