-- Search ORs a name match with an address substring match. Postgres can only
-- use indexes for an OR when every branch has one; without this, each
-- keystroke scans the whole establishment table.
create index establishment_address_trgm_idx on establishment using gin (address gin_trgm_ops);
