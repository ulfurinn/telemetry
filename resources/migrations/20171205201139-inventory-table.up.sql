CREATE TABLE riemann_inventory (
	service text not null,
	host text,
	description text,
	tags text[],
	last_seen timestamp with time zone,
	PRIMARY KEY (service, host)
);
