# Research — Agbale V1 (Phase 0)

All versions below were verified against Maven Central / npm on 2026-11-25 (latest **stable**
release; milestones, betas and release candidates excluded).

## R1. Template engine — Pebble 4.1.3

- **Decision**: Pebble `io.pebbletemplates:pebble:4.1.3` (BSD-3-Clause).
- **Rationale**: Jinja/Twig-like syntax that front-end developers already know; layout
  inheritance (`extends`/`block`), includes, macros; auto-escaping **on by default**;
  `strictVariables`; `maxRenderedSize` (output size cap); pluggable `MethodAccessValidator`,
  `Loader` and `PebbleCache`; thread-safe compiled templates. Actively maintained (4.1.3 released
  Nov 2026). Only runtime dependencies: `unbescape` (Apache 2.0) and `slf4j-api` (MIT).
- **Alternatives**: Thymeleaf (heavier, natural-templating syntax less natural for loops in
  tables, larger dependency set); FreeMarker (powerful but its default object wrapper exposes
  Java methods — riskier to sandbox); Mustache (too limited: no layout inheritance, no filters).

## R2. HTML → PDF — OpenHTMLtoPDF community 1.1.117 + jsoup 1.24.1

- **Decision**: `io.github.openhtmltopdf:openhtmltopdf-pdfbox:1.1.117` (LGPL-2.1-or-later;
  brings PDFBox 3.0.8, Apache 2.0) and `org.jsoup:jsoup:1.24.1` (MIT) to parse HTML5.
- **Rationale**: the only maintained open source Java engine with CSS paged media
  (`@page`, margin boxes, `counter(page)`/`counter(pages)`, `-fs-table-paginate` for repeated
  table headers). The community fork is active (1.1.117 released 2026-11-23). It exposes exactly
  the hooks our security model needs:
  - `useExternalResourceAccessControl(BiPredicate<String, ExternalResourceType>, priority)` →
    SSRF allow-list;
  - `useUriResolver` + `useProtocolsStreamImplementation` → a private classpath scheme;
  - `usePageSupplier(PageSupplier)` → count pages **while** rendering and abort early;
  - `useFont(FSSupplier<InputStream>, …)` → embedded fonts from the classpath.
- **Why jsoup**: OpenHTMLtoPDF natively expects well-formed XHTML parsed with a JDK XML parser.
  Templates are *standard HTML* (`<br>`, `<meta charset>`, unclosed `<p>`). jsoup parses HTML5
  like a browser and converts it to a W3C DOM (`W3CDom`) passed with `withW3cDocument`. jsoup is
  not an XML parser and never resolves DTDs or external entities → **XXE is impossible by
  construction** (FR-013). The DOM is never re-parsed by an XML parser.
- **Licence check (LGPL)**: LGPL-2.1 is compatible with distributing an Apache-2.0 library that
  *depends on* it as a separate, unmodified, replaceable JAR (dynamic linking through the Maven
  dependency). We do not shade, copy or modify OpenHTMLtoPDF. Documented in ADR-0003 and in the
  README FAQ.
- **Alternatives**: Flying Saucer (older, less CSS support, same lineage); iText 7 pdfHTML (AGPL
  or commercial — incompatible); headless Chrome (external process, heavy in Docker).

## R3. Embedded font — Noto Sans 2.004

- **Decision**: Noto Sans Regular / Bold / Italic / BoldItalic, version 2.004 (SIL OFL 1.1),
  ~2.1 MB in total, shipped in `agbale-core` under
  `io/github/juste120/agbale/fonts/` with `OFL.txt`.
- **Verification**: glyph coverage checked with fontTools on all four files: French diacritics,
  `Œ œ`, `€`, narrow no-break space (U+202F, used by French number formatting), em dash, and the
  Ewe letters `Ɖ ɖ Ɛ ɛ Ƒ ƒ Ɣ ɣ Ŋ ŋ Ɔ ɔ Ʋ ʋ` + combining tilde (U+0303): **all present**.
- **Rationale**: OFL allows bundling/redistribution inside software (the font must not be sold
  alone and keeps its licence); widely trusted; complete Latin/Greek/Cyrillic coverage.
- **Fallback strategy**: the font is registered under the families `Noto Sans`, `sans-serif`,
  `serif` and `monospace`, and the default stylesheet sets `font-family: 'Noto Sans',
  sans-serif` on `body`. So generic families and unspecified fonts never fall back to PDF base-14
  fonts (which cannot encode `ŋ`, `ɖ`…). Identical output with or without system fonts (SC-006).
- **Alternatives**: DejaVu Sans (fine coverage, but heavier and dated design); Inter (no Ewe
  coverage for all letters in older versions); Liberation (GPL with font exception — avoid).

## R4. Data exposure and template sandbox

- **Decision**: the `TemplateModelMapper` converts the user's data (record / POJO / Map) into an
  **immutable tree of safe values** before it reaches Pebble: `Map<String,Object>`, `List`,
  `String`, `Boolean`, `Number` (`BigDecimal`, `Integer`, `Long`…), `java.time` values,
  `java.util.Date`, enums (as their name), `null`. Root properties become top-level variables;
  a non-object root is exposed as `data` (FR-002).
- Pebble is then configured with a **deny-all** `MethodAccessValidator`, `strictVariables(true)`,
  `autoEscaping(true)` (HTML strategy), `allowOverrideCoreOperators(false)`,
  `maxRenderedSize(…)`, and our own `Loader` (no `FileLoader`, no `ClasspathLoader` outside the
  root).
- **Rationale**: two independent layers (defence in depth): even if a validator bug existed,
  templates never hold a reference to an arbitrary Java object, a `Class`, a `ClassLoader` or a
  service. Map keys and list indexes are resolved by Pebble's map/list resolvers, which do not
  invoke methods. This satisfies FR-014/FR-015 and keeps templates data-only.
- **Cycles / depth**: the mapper detects reference cycles (identity set) and limits depth (32)
  → clear `RenderException`.
- **Trade-off**: templates cannot call methods on data (`{{ invoice.total() }}`); this is
  intended — computations belong in the Response DTO or in filters. Documented in the user guide.

## R5. Resource loading & SSRF

- **Decision**: relative URIs in templates (`images/logo.png`, `css/print.css`) are resolved
  against a private base URI `agbale-classpath:/<templates-root>/<template-dir>/` and served by an
  `FSStreamFactory` that reads **only** from the class loader, below the templates root, with the
  same path validation as template names. `data:` URIs are handled natively.
- Every external access goes through `useExternalResourceAccessControl` (priority
  `RUN_AFTER_RESOLVING_URI`) with an `ResourceAccessPolicy`: allow `agbale-classpath:` and
  `data:`; allow `https://<host>` only if the host is allow-listed (exact match, no wildcard
  except an explicit `*.` sub-domain prefix, logged as WARN); deny everything else (`http`,
  `file`, `jar`, `ftp`, unknown).
- OpenHTMLtoPDF silently skips denied resources, so a per-render `ResourceIssues` collector
  records refused and missing URIs; after rendering, refused → `ResourceRefusedException` always;
  missing → `ResourceMissingException` in `FAIL` mode, WARN log in `WARN` mode (FR-034).
- **Alternatives**: rewriting URLs in the DOM before rendering (fragile: CSS `url()`, `@import`,
  `@font-face` would escape).

## R6. Resource limits

- **Template size** (default 1 MiB): enforced by the template resolver while reading (bounded
  read), for each template, layout and include.
- **Rendered HTML size**: Pebble `maxRenderedSize` = 8 × max template size (internal, derived) to
  stop runaway loops before the PDF step.
- **Pages** (default 500): custom `PageSupplier` throws an internal `PageLimitReached` error on
  page N+1 → `LimitExceededException(PAGES)` — layout stops early.
- **Time** (default 30 s): the whole render runs on a bounded pool of daemon threads
  (`Executors` + `Future.get(timeout)`); on timeout the future is cancelled and
  `LimitExceededException(RENDER_TIME)` is thrown. Limitation (documented): the JVM cannot force
  a busy thread to stop; the page and size limits bound the remaining work.

## R7. Spring Boot — 4.2.0 only (clarified)

- **Decision**: build the starter against Spring Boot **4.2.0** (latest stable, 2026-11-19),
  support declared for the 4.x line, tested on 4.2. Java 17 baseline (same as Boot 4).
- Boot 4 is modular: the starter depends on `spring-boot-autoconfigure`; web support is
  optional (`spring-webmvc`, `spring-boot-webmvc`, Jakarta Servlet) and activated with
  `@ConditionalOnWebApplication(type = SERVLET)` + `@ConditionalOnClass` (FR-033).
- `@Report` handling: a `HandlerMethodReturnValueHandler` **prepended** to the
  `RequestMappingHandlerAdapter` handlers (custom handlers added through `WebMvcConfigurer` run
  *after* `@ResponseBody` processing and would never see `@RestController` methods).
- Errors: `@RestControllerAdvice` (ordered first, scoped to Agbale exceptions) returning
  `ProblemDetail` with a `code` property (RFC 9457, `application/problem+json`).
- `Content-Disposition`: Spring `ContentDisposition` builder (RFC 6266 / RFC 5987 encoding of
  non-ASCII names with `filename*=UTF-8''…`).

## R8. Logging

- **Decision**: SLF4J API 2.0.20 in `agbale-core` (already required by Pebble, used by Spring
  Boot). No backend is imposed (FR-047). DEBUG per render; WARN for missing resources (warn mode)
  and wildcard allow-lists; no data values logged.

## R9. Build, quality and release tooling

| Concern | Choice (version) | Notes |
|---|---|---|
| Build | Maven 3.9+ (dev: 3.10.0), CI-friendly `${revision}` + flatten-maven-plugin 1.10.0 | single version for all modules |
| Compiler | maven-compiler-plugin 3.16.0, `--release 17`, `-Xlint:all -Werror` on main code | |
| Tests | JUnit 6.1.3 (BOM), AssertJ 3.27.7, ArchUnit 1.5.1, PDFBox 3.0.8 (text extraction) | JUnit 6 requires Java 17 ✔ |
| Spring tests | `ApplicationContextRunner`, MockMvc (Boot 4.2 test modules) | |
| Coverage | JaCoCo 0.8.16, check ≥ 0.80 line coverage on `agbale-core` | build fails below |
| Format | Spotless 3.12.0 + google-java-format 1.35.0 | runs on JDK 21+; 1.36+ needs commonmark, missing from the Spotless class path (ADR-0009) |
| Static analysis | SpotBugs 4.10.5 (plugin 4.10.5.0), effort Max, threshold Medium | Checkstyle not used: formatting is fully delegated to Spotless, avoiding overlapping rule sets (ADR-0009) |
| Javadoc | maven-javadoc-plugin 3.12.0, doclint on, internal packages excluded | |
| Security scans | OWASP dependency-check-maven 13.0.0 (`failBuildOnCVSS=9`), CodeQL, Dependabot | NVD API key as CI secret |
| Release | central-publishing-maven-plugin 0.12.0, maven-gpg-plugin 3.2.8, source 3.4.0 | `release` profile, secrets only in GitHub Actions |
| Demo API | Spring Boot 4.2.0, Spring Data JPA, H2, MapStruct 1.6.3 | never deployed (`maven.deploy.skip`) |
| Demo front | Angular 22.x (standalone components), Node ≥ 22 | built in its own CI job; local Node 18 is too old → documented |

## R10. Package visibility (no JPMS in V1)

- **Decision**: no `module-info.java` in V1. Public API = packages `api` (façade, DTOs,
  exceptions) plus the SPI packages `port` and `domain` (needed to plug custom resolvers and
  renderers, Principle IV). Packages `application` and `infrastructure` are **internal**: marked
  as such in `package-info.java`, excluded from Javadoc, no compatibility guarantee; helpers are
  package-private wherever wiring allows.
- **Rationale**: Pebble and OpenHTMLtoPDF are not fully modularised (automatic modules), which
  makes a strict `module-info` brittle for users on the module path; nearly all Spring Boot users
  run on the class path. Revisit before 1.0.0 (ADR-0005).

## R11. Composition and package dependencies

- `api` (façade/builder) is the **composition root**: `AgbaleBuilder.build()` wires
  infrastructure adapters into the application service. `application` uses `api` contract types
  only (records and exceptions), never `Agbale`/`AgbaleBuilder`. `infrastructure` may throw `api`
  exceptions. Enforced by ArchUnit (see plan, "Architecture rules").
