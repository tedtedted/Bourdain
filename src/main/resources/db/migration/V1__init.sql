-- pg_trgm powers fuzzy name search on the establishment list.
create extension if not exists pg_trgm;

-- One row per city license number as seen in the Food Inspections dataset.
-- A license number is the closest thing the city gives us to an establishment
-- identity, but it is NOT stable across ownership changes (license 2252464 was
-- Renaldi's Pizza before it was Duke of Perth), so inspections keep their own
-- dba_name snapshot and this row only reflects the most recent inspection.
create table establishment (
    license_number  bigint primary key,
    name            text not null,
    normalized_name text not null,
    aka_name        text,
    facility_type_raw text,
    facility_category text not null,
    risk            text not null,
    address         text not null,
    city            text,
    state           text,
    zip             text,
    latitude        double precision,
    longitude       double precision,
    -- Derived after each sync: OPEN, CLOSED, or RELOCATED (see RelocationService).
    status          text not null default 'OPEN',
    latest_result   text,
    last_inspected_on date,
    relocated_to_license_number bigint,
    relocated_to_address        text,
    relocated_since             date,
    created_at      timestamptz not null default now(),
    updated_at      timestamptz not null default now()
);

create index establishment_name_trgm_idx on establishment using gin (normalized_name gin_trgm_ops);
create index establishment_status_idx on establishment (status);
create index establishment_zip_idx on establishment (zip);

-- Primary key is the city's inspection_id. license_number links to
-- establishment logically; no FK because the two tables are owned by different
-- modules and rows can arrive in any order within a sync batch.
create table inspection (
    id              bigint primary key,
    license_number  bigint not null,
    dba_name        text not null,
    inspected_on    date not null,
    result          text not null,
    inspection_type text not null,
    inspection_type_raw text,
    violations_raw  text,
    updated_at      timestamptz not null default now()
);

create index inspection_license_date_idx on inspection (license_number, inspected_on desc);
create index inspection_result_date_idx on inspection (result, inspected_on desc);

-- Parsed out of the pipe-delimited `violations` blob on each inspection row.
create table violation (
    id            bigserial primary key,
    inspection_id bigint not null references inspection (id) on delete cascade,
    ordinal       int not null,
    code          int not null,
    description   text not null,
    comment       text
);

create index violation_inspection_idx on violation (inspection_id);

-- Mirror of the city's "Business Licenses - Current Active" dataset, filtered
-- to food-related license types. This is what lets us tell "moved" apart from
-- "closed": a closed establishment whose name reappears on an active license
-- at a new address has relocated. Fully refreshed each sync (the source
-- dataset drops rows when a license lapses, so watermarks don't apply).
create table business_license (
    record_id           text primary key,
    license_number      bigint not null,
    dba_name            text,
    normalized_name     text,
    legal_name          text,
    license_description text not null,
    address             text not null,
    city                text,
    state               text,
    zip                 text,
    license_start_date  date,
    expiration_date     date,
    status_raw          text,
    latitude            double precision,
    longitude           double precision,
    updated_at          timestamptz not null default now()
);

create index business_license_number_idx on business_license (license_number);
create index business_license_name_idx on business_license (normalized_name);

-- One row per sync attempt per source; the latest successful row carries the
-- watermark the next incremental run starts from.
create table sync_run (
    id            bigserial primary key,
    source        text not null,
    started_at    timestamptz not null,
    finished_at   timestamptz,
    status        text not null,
    rows_upserted int not null default 0,
    rows_skipped  int not null default 0,
    watermark     text,
    message       text
);

create index sync_run_source_idx on sync_run (source, started_at desc);
