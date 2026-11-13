# Migration plan: Spring Boot 3.5.5 → Spring Boot 4.1.x

Branch: `migration-to-spring-boot-4.0.0` (name predates the target choice below). Written 2026-11-13.

## 1. Target versions

| Item | Current | Target | Notes |
|---|---|---|---|
| Spring Boot | 3.5.5 | **4.1.x** (4.1.1 released 2026-08-20; check Maven Central for newer patch) | 4.1.0 GA 2026-06-10, includes all 4.0.7 fixes. Fallback: latest 4.0.x if a dependency isn't ready. |
| Java | 21 | **21 (keep)** — 17 is the minimum | The Boot 4.0 guide says "Java 17 or later, latest LTS encouraged". Java 25 is the newest LTS; moving to it is optional and should be a separate step. Local JDK is 24 (non-LTS) — use 21 or 25 for builds. |
| Spring Framework | 6.2 | 7.0.x | Jakarta EE 11, Servlet 6.1 |
| Spring Security | 6.5 | 7.0 / 7.1 | |
| Hibernate ORM | 6.6 | 7.x (7.4 in Boot 4.1) | |
| Flyway | 11 | 12.x | needs `spring-boot-starter-flyway` |
| Jackson | 2.x | 3.x (`tools.jackson`) | biggest code impact |
| Maven | 3.9.11 (wrapper) | fine | |

> Unverified: Boot 4.1 release notes don't state a Java baseline. Confirm in the 4.1 reference docs before changing `java.version`.

## 2. Findings in this project

Impact areas found by scanning `pom.xml`, `src/` and the YAML configs:

| # | Area | Where | Risk |
|---|---|---|---|
| 1 | `spring-boot-starter-web` renamed → `spring-boot-starter-webmvc` (old one deprecated) | `pom.xml` | Low |
| 2 | Flyway needs `spring-boot-starter-flyway`; bare `flyway-core` is no longer auto-configured. Also Flyway 12 needs the PostgreSQL module (`flyway-database-postgresql`) — verify | `pom.xml`, `application*.yml` | **High** (migrations silently skipped, `ddl-auto: validate` then fails) |
| 3 | Test starters per technology: `spring-boot-starter-webmvc-test`, `-data-jpa-test`, `-security-test` (for `@WithMockUser`); `spring-security-test` may be redundant | `pom.xml`, 8 test classes | Medium |
| 4 | `@MockBean` removed → `@MockitoBean` (`org.springframework.test.context.bean.override.mockito`) | `BranchControllerSliceTest`, `ReservationControllerTest`, `ReservationSearchErrorHandlingTest` | Medium |
| 5 | `@SpringBootTest` no longer auto-configures MockMvc/TestRestTemplate; slice annotations moved packages (`@WebMvcTest`, `@DataJpaTest`, `@AutoConfigureMockMvc` now live in the new `spring-boot-*-test` modules, e.g. `org.springframework.boot.webmvc.test.autoconfigure`) | all controller/repository tests | Medium |
| 6 | Jackson 3: `com.fasterxml.jackson.databind.ObjectMapper` → `tools.jackson.databind.json.JsonMapper`/`ObjectMapper`; Boot auto-configures `JsonMapper`. `jackson-annotations` (`@JsonProperty` in `LoginResponseDto`) keeps the old package. Jackson 3 exceptions are unchecked; some defaults changed (e.g. date/time serialization, `FAIL_ON_UNKNOWN_PROPERTIES`) — check JSON contract tests | 5 test classes, `LoginResponseDto`, any `spring.jackson.*` props | Medium (API output could change subtly) |
| 7 | springdoc `2.7.0` is Boot 3 only → **springdoc 3.x** (`springdoc-openapi-starter-webmvc-ui`). It generates OpenAPI 3.1 by default and has its own Jackson 3 dependency | `pom.xml`, `springdoc:` config | Medium |
| 8 | Testcontainers BOM `1.20.2` pinned in `pom.xml`; Boot 4 manages Testcontainers 2.x, whose artifacts are renamed: `junit-jupiter` → `testcontainers-junit-jupiter`, `postgresql` → `testcontainers-postgresql`. Remove the pinned BOM and rename artifacts | `pom.xml` (no `org.testcontainers` imports in Java, so likely unused) | Low |
| 9 | Hibernate 7: `@Where` (deprecated since 6.3) on `Car` → `@SQLRestriction("deleted = false")`. Do this *before* upgrading so it's verified on 6.6 | `Car.java:18` | Medium |
| 10 | Hibernate 7: explicit `database-platform: ...PostgreSQLDialect` / `H2Dialect` is unnecessary and discouraged → remove, let Hibernate detect. H2 dialect/version compatibility with Hibernate 7.4 must be tested (local profile uses H2 in PostgreSQL mode) | `application-dev/prod/local.yml` | Medium |
| 11 | Spring Security 7: lambda DSL already used (good). Check removed deprecated APIs, path-matching behaviour (`/api/v1/reservations/*/status` pattern, `PathPatternRequestMatcher` default), `/h2-console/**` matcher, authorization changes. `JwtAuthenticationFilter` is a custom filter — re-verify it | `SecurityConfig.java` | Medium |
| 12 | Actuator: liveness/readiness probes now enabled by default; `management.*` config reviewed. Endpoint exposure unchanged | `application*.yml` | Low |
| 13 | H2 console (`spring.h2.console`) auto-config moved into its own module/starter — verify it still works in `local` profile | `application-local.yml` | Low |
| 14 | `nimbus-jose-jwt 9.41.1` pinned: still works with Java 21, but upgrade to current 10.x is advisable. `JwtService` uses `MACSigner/MACVerifier/SignedJWT` — API stable. Spring Security 7 ships its own Nimbus; verify no version clash | `pom.xml`, `JwtService.java` | Low |
| 15 | MapStruct `1.6.3` + Lombok processor path: keep the `annotationProcessorPaths`; verify Lombok version managed by Boot 4 works with JDK in use and add `lombok-mapstruct-binding` if mapper/Lombok ordering problems appear | `pom.xml`, 5 mappers | Low |
| 16 | `spring-boot-maven-plugin`: layertools/CLASSIC loader removed (not used here). `-DskipTests` no longer skips test AOT processing — only `maven.test.skip` does | `pom.xml` | Low |
| 17 | Everything deprecated in 3.x/4.0 is **removed** in 4.x — compile with `-Xlint:deprecation` on 3.5.x first | whole project | Medium |
| 18 | Repo clutter: two `hs_err_pid*.log` JVM crash logs in the project root — find out why the JVM crashed (possibly JDK 24 + something) and don't commit them | repo root | Info |

## 3. Plan

Each phase ends with a green `./mvnw verify` and its own commit.

### Phase 0 — Baseline (on 3.5.x)
1. Use JDK 21 (or 25) for the build; confirm `./mvnw clean verify` is green and record the test count as the reference.
2. Bump to the latest 3.5.x patch.
3. Compile with deprecation warnings (`-Xlint:deprecation`) and fix all of them, notably `@Where` → `@SQLRestriction` (#9).
4. Optionally run the OpenRewrite recipe `org.openrewrite.java.spring.boot4.UpgradeSpringBoot_4_0` in a scratch branch to see the diff it would make; use as a reference, not as the migration itself.

### Phase 1 — Build file
1. Parent → `4.1.x`.
2. Starters: `web` → `webmvc`; add `spring-boot-starter-flyway` (and `flyway-database-postgresql` if required); add test starters (`webmvc-test`, `data-jpa-test`, `security-test`); drop `spring-security-test` if redundant.
3. springdoc → 3.x; remove Testcontainers BOM pin and rename artifacts (or delete them if unused).
4. Optional bridge: `spring-boot-starter-classic` / `spring-boot-starter-test-classic` to get compiling quickly, then remove. Add `spring-boot-properties-migrator` (runtime) temporarily to surface renamed properties; remove at end.

### Phase 2 — Main code
1. Fix imports/compilation (Jackson annotations, Security 7 API changes).
2. Remove `database-platform` from YAML profiles; review `spring.jackson.*`, `management.*`.
3. Review `SecurityConfig` + `JwtAuthenticationFilter` against Spring Security 7 migration guide.
4. Start the app in `local` (H2) and `dev` (PostgreSQL) profiles; confirm Flyway V1–V3 apply and Hibernate `validate` passes.

### Phase 3 — Tests
1. `@MockBean` → `@MockitoBean`; fix slice-test imports and add `@AutoConfigureMockMvc` where `@SpringBootTest` relied on it.
2. `ObjectMapper` → Jackson 3 `JsonMapper`/`ObjectMapper` in the 5 test classes.
3. Run full suite; investigate any JSON-shape differences (dates, nulls, BigDecimal) against `docs/search-api-documentation.md`.

### Phase 4 — Verification
1. Swagger UI loads at configured path; `/api-docs` JSON valid; compare with pre-migration output (diff of OpenAPI JSON).
2. Manual/API smoke: login → JWT → role-protected endpoints (ADMIN/EMPLOYEE/CUSTOMER) → reservation search/pagination.
3. Actuator `health`, `info`, `metrics` reachable as before; probe endpoints behave.
4. Remove classic starters and properties-migrator; final `./mvnw clean verify`.

### Phase 5 — Optional follow-ups (separate PRs)
- Java 21 → 25 LTS.
- Replace `nimbus-jose-jwt` hand-rolled JWT with Spring Security's resource server support.
- Replace `RuntimeException` wrapping in `JwtService`.

## 4. Rollback
Work stays on the migration branch; `main` untouched until Phase 4 passes. Each phase is a separate commit so it can be reverted independently.

## 5. Sources
- [Spring Boot 4.0 Migration Guide (wiki)](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)
- [Spring Boot 4.1 Release Notes (wiki)](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.1-Release-Notes)
- [Spring Boot 4.1.0 announcement](https://spring.io/blog/2026/06/10/spring-boot-4/)
- [Spring Boot 4.1.1 announcement](https://spring.io/blog/2026/08/20/spring-boot-4-1-1-available-now/)
- [Spring Boot 4: Everything You Need to Know (Dan Vega)](https://www.danvega.dev/blog/spring-boot-4-is-here)
- [The Ultimate Guide to Spring Boot 4 Migration](https://stevenpg.com/posts/ultimate-guide-spring-boot-4-migration/)
- [Spring Boot 4.0 migration for production teams (DEV)](https://dev.to/aytronn/spring-boot-40-migration-guide-for-production-teams-what-actually-breaks-and-how-to-upgrade-safely-22me)
- [springdoc-openapi v3.0.1 release](https://newreleases.io/project/github/springdoc/springdoc-openapi/release/v3.0.1)
- [OpenRewrite: Testcontainers 2 dependency renames](https://docs.openrewrite.org/recipes/java/testing/testcontainers/testcontainers2dependencies)
- [Moderne: Spring Boot 4.x recipes](https://docs.moderne.io/user-documentation/recipes/recipe-catalog/java/spring/boot4/)

## 6. Outcome (2026-11-13)

Branch `migration-to-spring-boot-4.1`; Spring Boot **4.1.2**, **Java 25**, built with JDK 25.0.5.

| Phase | Commit | Result |
|---|---|---|
| 0 Baseline on 3.5.16 | `@Where` → `@SQLRestriction` | 96 tests, 13 failures (same 13 on original 3.5.5) |
| 1-2 Build file + main code | starters, springdoc 3.2.0, Testcontainers renames, `SecurityConfig` | compiles |
| 3 Tests | `@MockitoBean`, new slice-test packages, Jackson 3 `ObjectMapper` | 96 tests, same 13 failures |
| 3b ProblemDetail | explicit `type=about:blank` | unchanged API response |
| 4 Verification | app run on H2 (local + non-local security chain), Swagger, actuator, register/login/JWT | OK |

Deviations from the plan / findings:
- **H2 pinned to 2.3.232** (`h2.version`): H2 2.4 (Boot 4.1 default) rejects Hibernate 7's ENUM-bound inserts into the Flyway VARCHAR+CHECK columns (24 `@DataJpaTest` errors). Root cause not investigated further.
- **`spring.jackson.visibility.creator: none`**: Jackson 3 auto-detects Lombok `@AllArgsConstructor` and nulls missing fields (e.g. `CarRequestDto.status` default lost). The setting restores default-constructor + setter binding.
- **ProblemDetail**: Spring 7 omits `type` when it is `about:blank`; handlers set it explicitly.
- `flyway-database-postgresql` added (required for PostgreSQL with Flyway 10+; was missing before).
- `nimbus-jose-jwt` 9.41.1 pin left as is (works).
- Not done: `database-platform` removal in YAML (still only a Hibernate warning), Testcontainers dependencies are unused in code.

Pre-existing issues noticed, not fixed:
- 13 failing tests in `ReservationRepositoryTest` (seed data from Flyway breaks expected counts).
- `SecurityConfig` rules match `/api/v1/branches|cars|reservations/**`, but controllers are mapped at `/api/...`; role rules therefore never apply (any authenticated user, e.g. CUSTOMER, can list branches/reservations).
- Seeded `admin`/`admin123` login returns bad credentials (BCrypt seed hash not verified).
- Local/test profile permits all requests; `/api/v1/auth/me` returns 500 there (no authentication).
