-- Hibernate can batch inserts only for sequence-generated ids; a bigserial
-- column read as IDENTITY forces one round trip per row. Step the existing
-- sequences by the allocation size the entities reserve at a time.
alter sequence violation_id_seq increment by 50;
alter sequence inspection_revision_id_seq increment by 50;
alter sequence establishment_revision_id_seq increment by 50;

-- Delisting marks licenses a completed sync run didn't list. Stamping the run
-- that last saw each license replaces comparing updated_at with the run's
-- start, which needed both timestamps from the database clock.
alter table business_license add column last_seen_sync_run_id bigint;

-- Several inspections can share an establishment's latest date with different
-- details. Remembering which inspection the details came from breaks the tie,
-- so replaying unchanged data no longer flips between them and writes a
-- revision each time. Seeded with the highest id on that date, the one that
-- wins the tie from now on.
alter table establishment add column latest_inspection_id bigint;
update establishment e
set latest_inspection_id = (
    select max(i.id) from inspection i
    where i.license_number = e.license_number and i.inspected_on = e.last_inspected_on
);
