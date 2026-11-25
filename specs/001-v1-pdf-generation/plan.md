# Implementation Plan: DocMint V1 — One-Line PDF Generation

**Branch**: `001-v1-pdf-generation` | **Date**: 2026-11-25 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/001-v1-pdf-generation/spec.md`

## Summary

Build `docmint-core` (pure Java 17 library: Pebble templates → sanitised HTML → OpenHTMLtoPDF PDF,
with FCFA/money/sum/date filters, embedded Noto Sans, default print CSS, thread-safe cache,
secure-by-default resource and template handling), `docmint-spring-boot-starter` (Spring Boot 4.2
auto-configuration, `@Report` return-value handler, RFC 9457 errors), `docmint-bom`, two
unpublished demos (Spring API + Angular), and the full open source project scaffolding (docs,
ADRs, CI, release pipeline). Technical choices and their verification are in
[research.md](research.md).

## Technical Context

**Language/Version**: Java, `--release 17`; built and tested on JDK 17 and 21 (dev on 21).

**Primary Dependencies**: Pebble 4.1.3, OpenHTMLtoPDF 1.1.117 (PDFBox 3.0.8), jsoup 1.24.1,
SLF4J API 2.0.20; starter: Spring Boot 4.2.0. Demo: Spring Data JPA, H2, MapStruct 1.6.3,
Angular 22.

**Storage**: N/A (library). Demo API: H2 in-memory.

**Testing**: JUnit 6.1.3, AssertJ 3.27.7, PDFBox text extraction, ArchUnit 1.5.1,
`ApplicationContextRunner`, MockMvc; JaCoCo 0.8.16 (≥ 80 % lines on core).

**Target Platform**: any JVM ≥ 17 (Linux/Windows/macOS, minimal Docker images without fonts).

**Project Type**: library (Maven multi-module) + Spring Boot starter + demos.

**Performance Goals**: 1–3 page invoice < 1 s warm (SC-004); 50 concurrent renders correct
(SC-005).

**Constraints**: zero mandatory config; no network/filesystem access by default; default limits
1 MiB template, 500 pages, 30 s; core has no Spring dependency.

**Scale/Scope**: V1 = ~45 production classes in core, ~10 in starter, 3 demo templates.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design: ✅ PASS.*

| Principle | How the plan complies | Status |
|---|---|---|
| I. Exemplary OSS | All files of FR-042 are tasks; ADR-0001…0010 record each decision below | ✅ |
| II. Simplicity | `DocMint.createDefault().pdf(name, data)` and `@Report`; every setting defaulted | ✅ |
| III. Standard HTML/CSS | HTML5 parsed by jsoup; print rules in standard CSS paged media | ✅ |
| IV. Escape hatches | `TemplateResolver` / `DocumentRenderer` SPI, `render(RenderRequest)` | ✅ |
| V. Modular clean architecture | Modules & packages below; ArchUnit rules; renderers registered per `OutputFormat` | ✅ (see note on composition root) |
| VI. Quality gates | JUnit/AssertJ/PDFBox/ArchUnit, JaCoCo ≥ 80 %, Javadoc doclint, Spotless, SpotBugs, CI 17 + 21 | ✅ |
| VII. Secure by default | R4 (safe model + deny-all methods + strict + autoescape), R5 (classpath scheme + allow-list), jsoup (no XXE), R6 limits, Problem Details, CodeQL/Dependabot/Dependency-Check, secret scan | ✅ |
| VIII. Actionable errors | exception hierarchy with codes, suggestions, line numbers; no paths | ✅ |
| IX. Francophone Africa | `fr` default, `fcfa`, Noto Sans with Ewe coverage | ✅ |
| X. SemVer, immutable releases | `${revision}` 0.1.0, CHANGELOG, manual release job gated on green CI | ✅ |
| Workflow: scope guard | FR-046 list; nothing from V2+ in tasks | ✅ |

## Project Structure

### Documentation (this feature)

```text
specs/001-v1-pdf-generation/
├── plan.md  research.md  data-model.md  quickstart.md
├── contracts/ (java-api.md, spring-boot-starter.md)
├── checklists/requirements.md
└── tasks.md             # /speckit-tasks
```

### Source Code (repository root)

```text
pom.xml                                   # parent: versions, plugins, quality gates, release profile
mvnw, .mvn/wrapper/
docmint-bom/pom.xml
docmint-core/
├── pom.xml
└── src/
    ├── main/java/io/github/juste120/docmint/
    │   ├── api/            DocMint, DocMintBuilder, RenderRequest, RenderResponse, RenderOptions,
    │   │                   DocMintException (+ TemplateNotFound, TemplateSyntax, Render,
    │   │                   InvalidTemplateName, ResourceRefused, ResourceMissing, LimitExceeded)
    │   ├── domain/         TemplateName, OutputFormat, PageSettings, ResourceLimits,
    │   │                   MissingResourceMode, ResourceAccessPolicy, FontDefinition, RenderedDocument
    │   ├── port/           TemplateResolver, TemplateSource, TemplateEngine, DocumentRenderer,
    │   │                   RenderContext, TemplateCache
    │   ├── application/    ReportService, DefaultReportService, RendererRegistry,
    │   │                   TemplateModelMapper, RenderResponseMapper, TimeLimiter, NameSuggester
    │   └── infrastructure/
    │       ├── template/   ClasspathTemplateResolver, ConcurrentTemplateCache
    │       ├── pebble/     PebbleTemplateEngine, DocMintExtension, FcfaFilter, MoneyFilter,
    │       │               SumFilter, DateFilter, ResolverLoader, DenyAllMethodAccessValidator,
    │       │               PebbleCacheAdapter
    │       └── pdf/        OpenHtmlPdfRenderer, FontRegistry, ClasspathResourceResolver,
    │                       ResourceGuard, PageLimitingSupplier, DefaultStylesheet
    ├── main/resources/io/github/juste120/docmint/
    │   ├── fonts/          NotoSans-{Regular,Bold,Italic,BoldItalic}.ttf, OFL.txt
    │   └── css/docmint-default.css
    └── test/java/…         unit, integration (PDF text), security/, architecture/, concurrency
docmint-spring-boot-starter/
└── src/main/java/io/github/juste120/docmint/spring/
    ├── DocMintAutoConfiguration, DocMintProperties
    └── web/  Report, Disposition, DocMintWebAutoConfiguration, ReportReturnValueHandler,
              ReportFileNameResolver, DocMintProblemDetailsHandler, ReturnValueHandlerInstaller
docmint-examples/
├── demo-api/      controller, dto/request, dto/response, mapper (MapStruct), service(+impl),
│                  entity, repository; resources/reports/{layouts/base.html, facture.html,
│                  recu.html, attestation.html, images/logo.png}
└── demo-angular/  src/app/{report.service.ts, documents.component.ts}
docs/  user-guide.md, architecture.md (Mermaid diagrams), adr/0001…0010
.github/  workflows/{ci.yml, codeql.yml, release.yml}, dependabot.yml, ISSUE_TEMPLATE/, PULL_REQUEST_TEMPLATE.md
README.md LICENSE NOTICE CHANGELOG.md ROADMAP.md CONTRIBUTING.md CODE_OF_CONDUCT.md SECURITY.md CLAUDE.md
```

**Structure Decision**: Maven multi-module modular monolith exactly as imposed by the
constitution (Principle V). Infrastructure is split into three sub-packages by adapter
(template loading, Pebble, PDF) so each adapter's helpers stay package-private.

## Key design decisions

1. **Render pipeline** (`DefaultReportService.render`):
   `TemplateName.of` → `TemplateModelMapper.toModel(data)` → (time-limited)
   `TemplateEngine.render(name, model, locale)` → HTML → `RendererRegistry.get(format)
   .render(html, context)` → `RenderedDocument` → `RenderResponseMapper` → `RenderResponse`.
2. **Pebble adapter**: our `ResolverLoader` implements Pebble's `Loader` on top of the
   `TemplateResolver` port, so `extends`/`include` names are validated by `TemplateName` too and
   read with the size limit. Pebble's cache is bridged to the `TemplateCache` port
   (`PebbleCacheAdapter`), disabled when `cache(false)`.
3. **PDF adapter**: jsoup → W3C DOM; inject `<style>` with the default stylesheet (generated
   from `PageSettings` + static CSS) as **first** child of `<head>` so template CSS overrides it;
   base URI `docmint-classpath:/<root>/<template-dir>/`; fonts from `FontRegistry`; access control
   + stream factory from `ResourceGuard`; `PageLimitingSupplier`; `useFastMode()`;
   producer `DocMint <version>`.
4. **Default stylesheet**: `@page { size: A4; margin: …; @bottom-center { content: "Page "
   counter(page) " / " counter(pages); font-size: 9pt; } }`, `body { font-family: 'Noto Sans',
   sans-serif; font-size: 10pt; }`, `.page-break { page-break-after: always; }`,
   `table { -fs-table-paginate: paginate; border-collapse: collapse; }`,
   `thead { display: table-header-group; }`, `tr { page-break-inside: avoid; }`.
5. **Error translation**: Pebble `ParserException` → `TemplateSyntaxException` (line, file);
   `PebbleException` with cause "not found" → `TemplateNotFoundException`; other evaluation errors
   → `RenderException(DOCMINT_TEMPLATE_EVALUATION)` with template name + line; messages built from
   Pebble's own message stripped of absolute paths.
6. **Spring**: `DocMintAutoConfiguration` (`@AutoConfiguration`, `@EnableConfigurationProperties`,
   `@ConditionalOnMissingBean DocMint`) — uses `ClassLoader` of the application context;
   `DocMintWebAutoConfiguration` (`@ConditionalOnWebApplication(SERVLET)`,
   `@ConditionalOnClass(RequestMappingHandlerAdapter)`) registers `ReturnValueHandlerInstaller`
   (`SmartInitializingSingleton` prepending `ReportReturnValueHandler`) and
   `DocMintProblemDetailsHandler` (`@RestControllerAdvice`, `@Order(HIGHEST_PRECEDENCE)`, handles
   only DocMint exceptions).

## Architecture rules (ArchUnit, `docmint-core/src/test/…/architecture`)

1. `domain` depends only on `java..`.
2. `port` depends only on `domain` and `java..`.
3. `application` depends only on `domain`, `port`, `api` contract types (records + exceptions,
   not `DocMint`/`DocMintBuilder`) and `java..`/`org.slf4j..`.
4. Only `infrastructure..` may depend on `io.pebbletemplates..`, `com.openhtmltopdf..`,
   `org.apache.pdfbox..`, `org.jsoup..`.
5. `infrastructure` does not depend on `application` nor on `DocMint`/`DocMintBuilder`.
6. No class in core depends on `org.springframework..`.
7. Every public class in `api` has Javadoc (checked by doclint) — and no `api` type exposes an
   infrastructure type in a public signature.

## ADRs to write (`docs/adr/`)

0001 record architecture decisions · 0002 Pebble as template engine · 0003 OpenHTMLtoPDF +
jsoup (incl. LGPL analysis) · 0004 Noto Sans embedded font · 0005 hexagonal packages, public
surface, no JPMS in V1 · 0006 Spring Boot 4.x only · 0007 template sandbox via safe data model ·
0008 RFC 9457 Problem Details · 0009 quality tooling (Spotless + SpotBugs, no Checkstyle) ·
0010 release through Maven Central Portal.

## CI/CD

- `ci.yml` (push/PR): matrix JDK 17 & 21 → `./mvnw -B verify` (tests, JaCoCo check, SpotBugs,
  Javadoc); JDK 21 job also runs `spotless:check` and OWASP Dependency-Check (`failBuildOnCVSS 9`,
  NVD key secret); gitleaks secret scan; demo-angular build (Node 22) as a separate job.
- `codeql.yml`: Java analysis, weekly + PR.
- `dependabot.yml`: Maven, npm (demo), GitHub Actions — weekly.
- `release.yml`: manual `workflow_dispatch` with version input, re-runs full verify, then
  `-P release deploy` (GPG key + Central token from secrets), modules core/starter/bom only.

## Complexity Tracking

| Deviation | Why needed | Simpler alternative rejected because |
|---|---|---|
| `api` → `infrastructure` dependency (composition root in `DocMintBuilder`) | zero-config one-liner must wire default adapters | a separate DI container or ServiceLoader adds machinery and hides wiring from readers |
| `application` → `api` contract types | spec places DTOs and exceptions in `api`; application returns them | duplicating DTOs/exceptions in application doubles the public surface for no gain |
| jsoup as extra dependency | HTML5 templates + XXE-proof parsing | XHTML-only templates violate Principle III |
