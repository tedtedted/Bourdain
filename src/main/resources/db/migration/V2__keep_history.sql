-- The city's datasets only describe the present: lapsed licenses vanish and
-- amended inspections are rewritten in place. Once a sync overwrites what we
-- knew, re-syncing cannot bring it back, so these tables keep it instead.

-- Licenses the source dataset stopped listing are marked, not deleted. Only
-- listed licenses count as relocation candidates.
alter table business_license add column delisted_at timestamptz;

-- The version of an inspection the city replaced, written just before the
-- amended row overwrites it. Violations can be re-parsed from violations_raw.
create table inspection_revision (
    id              bigserial primary key,
    inspection_id   bigint not null references inspection (id),
    license_number  bigint not null,
    dba_name        text not null,
    inspected_on    date not null,
    result          text not null,
    inspection_type text not null,
    inspection_type_raw text,
    violations_raw  text,
    superseded_at   timestamptz not null default now()
);

create index inspection_revision_inspection_idx on inspection_revision (inspection_id, superseded_at);

-- What an establishment looked like before a newer inspection changed its
-- name, address, or facility details — e.g. the previous owner of a reused
-- license number. last_inspected_on is the last inspection under that version.
create table establishment_revision (
    id                bigserial primary key,
    license_number    bigint not null references establishment (license_number),
    name              text not null,
    aka_name          text,
    facility_type_raw text,
    facility_category text not null,
    risk              text not null,
    address           text not null,
    city              text,
    state             text,
    zip               text,
    latitude          double precision,
    longitude         double precision,
    last_inspected_on date,
    superseded_at     timestamptz not null default now()
);

create index establishment_revision_license_idx on establishment_revision (license_number, superseded_at);

-- Append-only status timeline, one row each time derivation changes an
-- establishment's status or relocation target. recorded_at is when Bourdain
-- noticed, not when the business closed or moved; an establishment's first row
-- is simply its status when history began.
create table establishment_status_change (
    id                          bigserial primary key,
    license_number              bigint not null references establishment (license_number),
    status                      text not null,
    relocated_to_license_number bigint,
    relocated_to_address        text,
    relocated_since             date,
    recorded_at                 timestamptz not null default now()
);

create index establishment_status_change_license_idx on establishment_status_change (license_number, id desc);
