# Tasks: DocMint V1 — One-Line PDF Generation

**Input**: design documents in `specs/001-v1-pdf-generation/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: mandatory (constitution Principle VI). Test tasks precede or accompany the code they
cover. Paths are relative to the repository root; `core` = `docmint-core/src/main/java/io/github/juste120/docmint`,
`core-test` = `docmint-core/src/test/java/io/github/juste120/docmint`,
`starter` = `docmint-spring-boot-starter/src/main/java/io/github/juste120/docmint/spring`.

Format: `- [ ] [ID] [P?] [Story?] Description` — `[P]` = parallelisable (different files, no
dependency on an unfinished task).

## Phase 1: Setup

- [x] T001 Create parent `pom.xml` (revision 0.1.0-SNAPSHOT, `--release 17`, dependency & plugin management with versions from research.md R9, modules, SCM/licence/developer metadata)
- [x] T002 Add Maven wrapper `mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties`
- [x] T003 [P] Create `docmint-bom/pom.xml` (core + starter, flatten)
- [x] T004 [P] Create `docmint-core/pom.xml` (Pebble, OpenHTMLtoPDF, jsoup, SLF4J; test: JUnit, AssertJ, ArchUnit, PDFBox, slf4j-simple; JaCoCo ≥ 0.80)
- [x] T005 [P] Create `docmint-spring-boot-starter/pom.xml` (Boot 4.2 autoconfigure, optional webmvc/servlet, configuration processor)
- [x] T006 Configure quality gates in parent: Spotless (google-java-format, JDK ≥ 21 profile), SpotBugs, Javadoc doclint, JaCoCo, enforcer (Java ≥ 17, Maven ≥ 3.9), `release` profile (sources, javadoc, gpg, central-publishing)
- [x] T007 [P] Add `.gitignore`, `.editorconfig`, `.gitattributes`

## Phase 2: Foundational (blocks all stories)

- [x] T008 [P] Domain value types in `core/domain/`: `TemplateName`, `OutputFormat`, `PageSettings`, `ResourceLimits`, `MissingResourceMode`, `ResourceAccessPolicy`, `FontDefinition`, `RenderedDocument` + `package-info.java`
- [x] T009 [P] Unit tests for domain types in `core-test/domain/` (TemplateNameTest incl. traversal cases, ResourceAccessPolicyTest, ResourceLimitsTest, PageSettingsTest)
- [x] T010 [P] Public exception hierarchy in `core/api/` (`DocMintException` + 7 subtypes with stable codes)
- [x] T011 [P] API records in `core/api/`: `RenderRequest`, `RenderOptions`, `RenderResponse`
- [x] T012 Ports in `core/port/`: `TemplateResolver`, `TemplateSource`, `TemplateEngine`, `DocumentRenderer`, `RenderContext`, `TemplateCache`
- [x] T013 Architecture tests in `core-test/architecture/ArchitectureTest.java` (rules 1–7 of plan.md)

**Checkpoint**: domain, contracts and architecture rules compile and pass.

## Phase 3: User Story 1 — PDF in one line (P1) 🎯 MVP

**Goal**: `DocMint.createDefault().pdf("invoice", data)` returns a PDF with the data.
**Independent test**: render `reports/invoice.html` from a record, a POJO and a Map; extract text.

- [x] T014 [P] [US1] `TemplateModelMapper` (record/POJO/Map → safe tree, `data` root rule, cycles, depth) in `core/application/TemplateModelMapper.java` + `core-test/application/TemplateModelMapperTest.java`
- [x] T015 [P] [US1] `ConcurrentTemplateCache` in `core/infrastructure/template/` + test
- [x] T016 [P] [US1] `ClasspathTemplateResolver` (bounded read, listing from directories and JARs) in `core/infrastructure/template/` + test
- [x] T017 [US1] Pebble adapter: `PebbleTemplateEngine`, `ResolverLoader`, `PebbleCacheAdapter`, `DenyAllMethodAccessValidator`, error translation in `core/infrastructure/pebble/`
- [x] T018 [US1] PDF adapter: `OpenHtmlPdfRenderer`, `DefaultStylesheet`, `FontRegistry` (Noto Sans) in `core/infrastructure/pdf/`; copy fonts + `OFL.txt` to `docmint-core/src/main/resources/io/github/juste120/docmint/fonts/`
- [x] T019 [US1] Application: `ReportService`, `DefaultReportService`, `RendererRegistry`, `RenderResponseMapper`, `TimeLimiter` in `core/application/`
- [x] T020 [US1] Façade `DocMint` + `DocMintBuilder` (composition root) in `core/api/`
- [x] T021 [US1] Integration tests `core-test/DocMintPdfIT`-style (`OnePdfLineTest`: variables, loops, conditions, layout inheritance, record/POJO/Map equivalence, defaults, cache on/off, `clearCache`) with templates in `docmint-core/src/test/resources/reports/`
- [x] T022 [US1] Test helper `core-test/support/PdfText.java` (PDFBox text extraction, page count, page size)

**Checkpoint**: MVP — one-line PDF works in plain Java.

## Phase 4: User Story 2 — Secure by default (P1)

**Independent test**: `core-test/security/*` suite, one class per threat.

- [x] T023 [US2] `ResourceGuard` (classpath scheme stream factory, access control, `ResourceIssues` collector) + `ClasspathResourceResolver` in `core/infrastructure/pdf/`
- [x] T024 [US2] `PageLimitingSupplier` + time limit wiring + template size limit errors
- [x] T025 [P] [US2] `PathTraversalTest` (names, encoded, absolute, backslash, NUL; includes/extends; resource paths)
- [x] T026 [P] [US2] `HtmlEscapingTest` (script/markup escaped; `raw` explicit)
- [x] T027 [P] [US2] `RemoteResourceTest` (http/https/file/jar refused; allow-listed host allowed — local HTTPS-free check via policy unit test + refusal integration test)
- [x] T028 [P] [US2] `XxeTest` (DOCTYPE with external entity / SYSTEM DTD not resolved)
- [x] T029 [P] [US2] `TemplateSandboxTest` (getClass, class loader, method calls, strict variables)
- [x] T030 [P] [US2] `ResourceLimitsTest` (template size, pages, render time)

## Phase 5: User Story 3 — Business formatting & print layout (P2)

- [x] T031 [P] [US3] `FcfaFilter` + test (rounding, negative, null, huge, numeric string)
- [x] T032 [P] [US3] `MoneyFilter` + test (EUR/XOF/USD, locales, null)
- [x] T033 [P] [US3] `SumFilter` + test (numbers, property path, nulls, empty, BigDecimal exactness)
- [x] T034 [P] [US3] `DateFilter` + test (LocalDate, LocalDateTime, Zoned/Offset, Instant, Date, pattern, locale)
- [x] T035 [US3] `DocMintExtension` registering filters; per-render locale
- [x] T036 [US3] `PrintLayoutTest` (A4 size, "Page X / Y", `.page-break`, repeated `thead`, CSS override)
- [x] T037 [US3] `FontAndImagesTest` (French + Ewe text extraction, no missing glyphs, classpath image, Data URI image, custom font registration, missing image fail/warn, malformed Data URI and unsupported image format reported as missing resource)

## Phase 6: User Story 4 — Spring Boot starter (P2)

- [x] T038 [US4] `DocMintProperties` + `DocMintAutoConfiguration` + `AutoConfiguration.imports` + metadata in `starter/`
- [x] T039 [US4] `Report`, `Disposition`, `ReportFileNameResolver` (placeholders + sanitising) in `starter/web/`
- [x] T040 [US4] `ReportReturnValueHandler` + `ReturnValueHandlerInstaller` + `DocMintWebAutoConfiguration`
- [x] T041 [US4] `DocMintProblemDetailsHandler` (RFC 9457, codes, 406)
- [x] T042 [P] [US4] `DocMintAutoConfigurationTest` (ApplicationContextRunner: defaults, properties, back-off, invalid values, non-web context)
- [x] T043 [P] [US4] `ReportEndpointTest` (MockMvc: inline/attachment, file names, non-ASCII, placeholders, injection, errors as problem+json, 406, null return)
- [x] T044 [P] [US4] `ReportFileNameResolverTest`

## Phase 7: User Story 5 — Actionable errors (P3)

- [x] T045 [US5] `NameSuggester` (Levenshtein) + not-found message with available templates
- [x] T046 [US5] `ErrorMessagesTest` (not found + suggestion, syntax line, undefined variable, missing image, self/mutual layout inheritance, no absolute path / stack trace in messages)

## Phase 8: User Story 6 — Discover, try, adopt (P3)

- [x] T047 [P] [US6] Demo API (`docmint-examples/demo-api`): entities, repositories, DTOs, MapStruct mappers, services, controllers, data seed, templates, logo, CORS exposing `Content-Disposition`, tests
- [x] T048 [P] [US6] Demo Angular (`docmint-examples/demo-angular`): `ReportService`, documents component with View/Download, README
- [x] T049 [P] [US6] `README.md` (EN + FR section, badges, install, 5-minute PDF, Spring + Angular, filters, FAQ, roadmap)
- [x] T050 [P] [US6] `LICENSE`, `NOTICE`, `CHANGELOG.md`, `ROADMAP.md`, `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`, `SECURITY.md`
- [x] T051 [P] [US6] `.github/`: `workflows/ci.yml`, `workflows/codeql.yml`, `workflows/release.yml`, `dependabot.yml`, issue templates, PR template
- [x] T052 [P] [US6] `docs/user-guide.md`, `docs/architecture.md` (Mermaid), `docs/adr/0001…0010`
- [x] T053 [P] [US6] `CLAUDE.md` (context, sources of truth, workflow, conventions, French explanations)

## Phase 9: Polish & cross-cutting

- [x] T054 Concurrency test (50 parallel renders identical to sequential) and performance smoke test (< 1 s warm)
- [x] T055 Logging (FR-047): DEBUG per render, WARN for warn-mode and wildcard hosts; test that data never appears in logs
- [x] T056 Full `./mvnw verify` on JDK 17 and 21; coverage ≥ 80 %; SpotBugs clean; Javadoc clean
- [x] T057 Run quickstart.md manually; update CHANGELOG `[0.1.0]` section (README quick start replayed in a blank project: first PDF in seconds; demo API run live, all endpoints OK. A check by someone new to the project — SC-001 — remains before release)

## Dependencies

- Setup (T001–T007) → Foundational (T008–T013) → US1 (T014–T022).
- US2, US3 depend on US1 (renderer/engine exist). US2 and US3 can run in parallel.
- US4 depends on US1 (and uses US5 error codes, which already exist from T010).
- US5 depends on US1. US6 depends on US1–US4 for demo/docs content.
- Polish last.

## Parallel examples

- Foundational: T008, T010, T011 together; then T009 and T012.
- US1: T014, T015, T016 together.
- US2: T025–T030 together once T023–T024 are done.
- US3: T031–T034 together.
- US6: T047–T053 together.

## Implementation strategy

MVP first (US1 → stop and validate), then security (US2) before anything is shown publicly, then
US3 and US4, then errors polish, docs and demos. Commit after each task group with Conventional
Commits.
