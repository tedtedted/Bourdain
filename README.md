# Bourdain

Chicago restaurant health inspections, searchable. Mirrors the city's
[Food Inspections](https://data.cityofchicago.org/Health-Human-Services/Food-Inspections/4ijn-s7e5)
and [Business Licenses (active)](https://data.cityofchicago.org/Community-Economic-Development/Business-Licenses-Current-Active/uupf-x98q)
datasets daily and serves a small search UI.
Built with **Java 25**, **Spring Boot 4**, **Spring Modulith**, **PostgreSQL**, **Flyway**, and **Thymeleaf + htmx**.

The reason for the second dataset: the inspections feed only knows about
licenses that have been inspected, so a restaurant that moves looks *closed*
forever (the Duke of Perth problem — closed on Clark St in 2024, thriving on
Broadway since 2025). Bourdain cross-references active licenses by normalized
name and marks those establishments **RELOCATED**, pointing at the new address.

## Modules

Spring Modulith application modules, verified by `ModularityTests`:

| Module | Responsibility |
|---|---|
| `civicdata` | Socrata sync: scheduling, watermarks, page fetch, `sync_run` bookkeeping |
| `inspection` | Inspection history, result/type enums, violation parsing |
| `establishment` | Establishment identity, active-license mirror, status + relocation derivation |
| `web` | Thymeleaf + htmx UI |

Fetched pages flow to the domain modules as in-transaction events; a persisted
`CivicDataSyncCompleted` event (Modulith JDBC registry) hands off to status
derivation and relocation matching after each run.

## Run locally

```bash
docker compose -f docker-compose-dev.yml up -d   # Postgres only
./mvnw spring-boot:run                           # syncs on startup, then http://localhost:8080
```

The first sync backfills ~300k inspections (minutes). A
[Socrata app token](https://dev.socrata.com/docs/app-tokens) in
`SOCRATA_APP_TOKEN` avoids throttling but isn't required.

## Tests

```bash
./mvnw verify
```

Integration tests provision their own `postgres:17` via Testcontainers, so
Docker is the only prerequisite. Coverage is enforced by JaCoCo; module
boundaries by `ModularityTests`.

## Deploy

Same scheme as vmb/chesapeake: CI runs `verify`; pushes to `main` publish
`ghcr.io/tedtedted/bourdain:edge` plus an immutable `sha-*` tag (version tags
add semver + `latest`). On the host:

```bash
cp .env.example .env   # then fill in
docker compose up -d
```

The app binds to `127.0.0.1` by default; set `BOURDAIN_BIND` to the host's
Tailscale IP (or use `tailscale serve`) to reach it from the tailnet.

## Notes

- `establishment` identity is the city license number, which is **not** stable
  across ownership changes — inspections keep their own name snapshot.
- Relocation matching is deliberately conservative: it requires exactly one
  plausible new location, so chains never match. Geographic disambiguation is
  future work.
