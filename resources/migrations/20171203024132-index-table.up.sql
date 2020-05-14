CREATE TABLE riemann_index (
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
--;;
CREATE INDEX ON riemann_index (host);
--;;
CREATE INDEX ON riemann_index (state);
