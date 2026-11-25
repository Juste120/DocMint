# CLAUDE.md — DocMint

Context for AI assistants (and humans) working on this repository.

## Project

DocMint is an open source Java library that *mints* (generates)
business documents (invoices, receipts, statements, attestations, certificates) as PDF from
HTML/CSS templates, in one line of Java, with a Spring Boot starter.
Slogan: *Business documents from HTML, in one line of Java.*

- Maintainer: PAKOU Komi Juste (GitHub `Juste120`), Java/Spring Boot + Angular developer in Lomé.
- groupId `io.github.juste120`, root package `io.github.juste120.docmint`, licence Apache 2.0.
- Versions: **0.1.0** (V1, "The PDF in one line", branch `001-v1-pdf-generation`) and **0.2.0**
  (V2, "Professional documents", branch `002-v2-professional-documents`). See `ROADMAP.md`.

## Sources of truth (in this order)

1. `.specify/memory/constitution.md` — non-negotiable principles.
2. `specs/<feature>/spec.md` — what to build (V1: `specs/001-v1-pdf-generation/`, V2:
   `specs/002-v2-professional-documents/`, V3: `specs/003-v3-charts-excel-csv/`).
3. `specs/<feature>/plan.md` (+ `research.md`, `data-model.md`, `contracts/`) — how.
4. `specs/<feature>/tasks.md` — task breakdown.

If they conflict, the higher one wins and the lower one must be corrected.

## Method: spec-driven development (Spec Kit)

`/speckit-constitution` → `/speckit-specify` → `/speckit-clarify` → `/speckit-plan` →
`/speckit-tasks` → `/speckit-analyze` → `/speckit-implement`. Never skip a step; get the
maintainer's explicit approval between steps unless he asks to chain them. Each future version
(0.2.0, …) gets its own spec. **Never implement a feature of a future version without an
explicit request.**

## Working with the maintainer

- **Explain every non-trivial technical choice in French** (he is learning while building and
  preparing the OCP Java 21 certification): what, why, alternatives, and the Java concept
  involved when relevant.
- Code, Javadoc, documentation, README and commit messages are in **English**; the README has a
  French section.
- Ask before irreversible or outward-facing actions (publishing, pushing, creating accounts).

## Repository layout

| Path | Content |
|---|---|
| `docmint-core` | Pure Java engine (no Spring). Packages `api` (public), `domain` + `port` (public SPI), `application` + `infrastructure` (internal) |
| `docmint-spring-boot-starter` | `DocMintAutoConfiguration`, `DocMintProperties`, `web/` (`@Report`, return value handler, RFC 9457 errors) |
| `docmint-excel` | Optional Excel export (Apache POI), discovered with `ServiceLoader` |
| `docmint-bom` | Version alignment |
| `docmint-examples/demo-api` | Layered demo API (controller, dto/request, dto/response, mapper, service/impl, entity, repository) — never published |
| `docmint-angular` | Angular workspace of the npm library `@juste120/docmint-angular` (Vitest tests) |
| `docmint-examples/demo-angular` | Angular demo using the library (View/Download/Preview) — never published |
| `docs/` | User guide, architecture, ADRs |
| `specs/`, `.specify/` | Spec Kit artifacts |

## Architecture rules (checked by `ArchitectureTest`)

- `domain` depends only on the JDK; `port` only on `domain`.
- `application` knows `port`, `domain` and the `api` contract types, never the façade.
- Only `infrastructure` imports Pebble, OpenHTMLtoPDF, PDFBox, jsoup.
- Core never depends on Spring. Starter → core; demo-api → starter.
- New formats/features = new adapters (Strategy, open/closed); do not modify the core for them.
- Templates receive response DTOs, never JPA entities.

## Security rules (constitution Principle VII — non-negotiable)

Template names validated (no traversal); no remote/file resources unless an HTTPS host is
allow-listed; jsoup parsing (no XXE); auto-escaping (`raw` explicit); templates only see safe
data and cannot call methods; limits on template size, rendered size, pages, time; errors
without stack traces, paths or data; no secrets in the repository. **Every security rule has a
test** in `docmint-core/src/test/java/io/github/juste120/docmint/security/`.

## Conventions

- Java 17 (`--release 17`), developed on JDK 21. Records for value types, `final` classes,
  immutable objects, constructor injection.
- Errors: throw an `DocMintException` subtype with a stable code and a message that says what to
  do; never log data values.
- Formatting: google-java-format via Spotless (`./mvnw spotless:apply`). No Checkstyle.
- Javadoc on every public API type/method.
- Tests: JUnit 6, AssertJ, PDFBox text extraction (`support/PdfText`), ArchUnit,
  `ApplicationContextRunner` and MockMvc for the starter. Coverage of `docmint-core` ≥ 80 %.
- Commits: Conventional Commits (`feat(core): …`, `fix(starter): …`, `docs: …`, `test: …`,
  `build: …`, `ci: …`, `chore: …`).
- Significant decision → new ADR in `docs/adr/`. User-visible change → `CHANGELOG.md`.

## Commands

```bash
./mvnw verify                                   # full quality gate (use JDK 21 locally)
./mvnw spotless:apply                           # format
./mvnw -pl docmint-core test -Dtest=FiltersTest  # one test class
./mvnw -pl docmint-examples/demo-api -am spring-boot:run   # demo API on :8080
cd docmint-angular && npm ci && npm test && npm run build  # Angular library (Node 22.22+/24.15+)
cd docmint-examples/demo-angular && npm ci && npm start    # Angular demo on :4200 (build the library first)
```

Release: GitHub Actions workflow `Release` (manual), never from a laptop.
