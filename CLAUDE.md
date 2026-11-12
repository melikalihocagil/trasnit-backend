# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

A Spring Boot backend for a public transit (GTFS-style) routing and stops management API — "transit" (region: Erzurum, Turkey). Serves route/stop data and computes route navigation geometry via an external OSRM routing engine.

## Commands

Build system is Gradle (wrapper included). Java toolchain: 25. Spring Boot 4.1.1.

```
./gradlew.bat build              # full build (compile + test)
./gradlew.bat test                # run all tests
./gradlew.bat test --tests "com.demo.transit.application.RoutingServiceTest"   # single test class
./gradlew.bat test --tests "com.demo.transit.application.RoutingServiceTest.shouldReturnEmptyNavigationWhenStopsIsNull"  # single test method
./gradlew.bat bootRun             # run the app locally
```

Tests use JUnit 5 (`useJUnitPlatform()`), Mockito, AssertJ, and Spring's `@WebMvcTest`/`@SpringBootTest` slices. The test profile (`src/test/resources/application.yaml`) runs against an in-memory H2 database instead of Postgres.

## Runtime dependencies

- **PostgreSQL** — primary datastore. `spring-boot-docker-compose` auto-starts the service defined in `compose.yaml` when running the app directly (disabled in tests via `spring.docker.compose.skip.in-tests`).
- **OSRM** (Open Source Routing Machine) — external HTTP service used to compute driving routes/geometry between stops. `compose.yaml` also defines an `osrm` service bound to a local `.osrm` data file; `osrm.base-url` (default `http://localhost:5000`) must point at a running instance for routing endpoints to work.

## Architecture

The codebase follows a hexagonal (ports & adapters) layering under `com.demo.transit`:

- **`application/`** — core domain/use-case layer. Contains the `@Service` classes (`RouteService`, `RoutingService`, `StopService`), domain models (`model/`, plain Java records like `Stop`, `Via`, `Route`, `Trip`, `RouteNavigation`, `Leg`, `Coordinate`), and **ports** (`port/`) — interfaces like `RouteRepository`, `StopRepository`, `TripRepository`, `ViaRepository`, `StopTimeRepository`, `RoutingEngine` that the application layer depends on but does not implement.
- **`infrastructure/`** — adapters implementing the ports.
  - `persistence/` — `Jdbc*Repository` classes implement the repository ports using Spring's `JdbcClient`/`JdbcTemplate` directly (no Spring Data JPA repositories, despite the JPA/Hibernate-Spatial dependencies being present). SQL is inlined as text blocks.
  - `osrm/` — `OsrmRoutingAdapter` implements `RoutingEngine` as an anti-corruption layer around the OSRM HTTP API (`RestClient`), translating OSRM's GeoJSON leg geometry into the domain's `RouteNavigation`/`Leg`/`Coordinate` model. `OsrmProperties` binds the `osrm.*` config prefix.
  - `config/` — cross-cutting infra config (`OpenApiConfig` for Springdoc/Swagger).
- **`presentation/`** — `controller/` (REST controllers under `/api/...`), `dto/` (request/response records, distinct from domain models), and `mapper/` (`TransitDtoMapper`, a MapStruct interface generating the `*Impl` mapper bean at compile time — tests that need it `@Import(TransitDtoMapperImpl.class)`).

Data flows: controller → application service (orchestrates one or more repository ports + `RoutingService`) → port interface → infrastructure adapter (JDBC or OSRM) → back up through DTO mapping to the HTTP response.

### Multi-tenant draft/published model

Route and stop editing works on a per-user draft basis before publishing. The database uses separate schemas — `published` and `draft` (see `JdbcRouteRepository`'s SQL and the H2 test init script `CREATE SCHEMA ... published/draft/history`). Services generally expose parallel `getPublished*` / `getDraft*` methods keyed by a `userId` string (passed via the `X-User-Id` request header, defaulting to `"operator-1"` — see `RouteController`/`StopController`). `RoutingService.hasGeometryChanged` compares published vs. draft `RouteNavigation` to detect if a draft edit actually altered the route geometry (used e.g. to figure out which routes are affected by a stop move).

### Routing/geometry

`RoutingService` builds an ordered coordinate list from a route's stops plus any `Via` points (non-stop waypoints that pin OSRM to the correct road/lane between two consecutive stops, ordered by `viaSequence`), then delegates to the `RoutingEngine` port (OSRM) to get back distance/duration/leg geometry. Stops and vias are plain lat/lon; OSRM expects `lon,lat` — conversion happens in `OsrmRoutingAdapter`.

## Notes

- CORS (`WebConfig`) currently only allows `http://localhost:5173` (local Vite frontend dev server).
- Some code comments and test `@DisplayName`s are written in Turkish — keep this consistent when touching existing files in those areas.
