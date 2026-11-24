# Agbale Constitution

Agbale ("Agbalẽ": "paper, document" in Ewe) is an open source Java library that generates
business documents from HTML/CSS templates, in one line of Java.
Slogan: *Business documents from HTML, in one line of Java.*

## Core Principles

### I. Exemplary Open Source

Agbale is a public open source project and MUST be exemplary for any developer who
discovers it.

- Code MUST be clean, well structured, readable and documented; a newcomer MUST be able to
  understand a module by reading its package layout, Javadoc and the architecture guide.
- Every significant technical decision MUST be recorded as an Architecture Decision Record
  in `docs/adr/` (context, decision, consequences, alternatives considered).
- The repository MUST contain and keep current: `README.md`, `LICENSE` (Apache 2.0),
  `CHANGELOG.md`, `ROADMAP.md` (no dates), `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`,
  `SECURITY.md`, `.github/` templates and workflows, `docs/` (user guide, architecture guide
  with diagrams, ADRs) and `CLAUDE.md`.
- Code, Javadoc, README, documentation and commit messages are written in English; the
  README additionally contains a French section.

**Rationale**: the project's credibility and its contributor base depend on how easy it is
to read, trust and extend.

### II. Simplicity First

- The common case MUST fit in one line of code: `agbale.pdf("invoice", data)`,
  `@Report("invoice")`, or `Agbale.builder().build().pdf("invoice", data)` without Spring.
- Configuration MUST be optional: every setting has a safe, documented default.
- A developer MUST be able to produce a first PDF in under 10 minutes using only the README.
- A feature that makes the common case longer or requires mandatory configuration MUST be
  rejected or redesigned.
- YAGNI: only the features of the version currently specified are built.

**Rationale**: simplicity is Agbale's core differentiator against JasperReports.

### III. Standard HTML/CSS Templates

- Templates MUST be plain HTML/CSS files (with Pebble syntax for data), editable and
  previewable with any standard IDE or browser.
- No proprietary editor, no project-specific XML format, no binary template format.
- Print layout (page size, margins, page numbers, page breaks) MUST be expressed with
  standard CSS (paged media), with sensible defaults provided by Agbale.

**Rationale**: every web developer already knows HTML/CSS; nothing new must be learned to
write a document.

### IV. Expert Escape Hatches

- Advanced users MUST be able to go beyond the defaults (advanced API, custom template
  resolvers, pluggable renderers and engines) through documented extension points.
- Extension points MUST NOT add any step, parameter or concept to the common case.
- Extensions are exposed through public interfaces in the `api` package or documented
  service ports, never by requiring users to depend on internal classes.

**Rationale**: simple by default, powerful when needed.

### V. Modular Clean Architecture

- The project is a Maven multi-module modular monolith:
  `agbale-core`, `agbale-spring-boot-starter`, `agbale-bom`,
  `agbale-examples/demo-api`, `agbale-examples/demo-angular`.
- Allowed module dependencies: starter → core; demo-api → starter. `agbale-core` MUST NEVER
  depend on Spring. Example modules MUST NEVER be published.
- `agbale-core` follows a hexagonal (Clean Architecture) layout with packages
  `api`, `domain`, `application`, `port`, `infrastructure`:
  - `domain` depends on nothing (no third-party library, no other Agbale package);
  - `application` depends only on `domain` and `port`;
  - only `infrastructure` may import third-party engines (Pebble, OpenHTMLtoPDF, PDFBox…);
  - only the `api` package is public API; everything else is package-private or explicitly
    internal.
- These rules MUST be enforced by ArchUnit tests that fail the build when violated.
- Each new output format or feature MUST be added as a new adapter (Strategy pattern,
  open/closed principle) without modifying the application core.
- The demo API MUST pass Response DTOs to templates, never JPA entities.

**Rationale**: a stable core and replaceable adapters let the project grow across versions
without rewrites.

### VI. Quality Gates (NON-NEGOTIABLE)

- Every change MUST come with automated tests (JUnit 5, AssertJ; PDF content verified by text
  extraction; starter verified with `ApplicationContextRunner` and MockMvc; architecture
  verified with ArchUnit).
- Line coverage of `agbale-core` MUST be ≥ 80 %, enforced by the build.
- Every public type and method of the public API MUST have Javadoc; the Javadoc build MUST
  succeed without errors.
- Formatting and static analysis MUST run automatically in the build (e.g. Spotless plus
  Checkstyle and/or SpotBugs); violations fail the build.
- CI MUST run on Java 17 and Java 21. Nothing is merged to the main branch or released while
  CI is red.

**Rationale**: a published library is used in other people's production systems; quality
cannot be added afterwards.

### VII. Secure by Default (NON-NEGOTIABLE)

No feature may open a vulnerability in the applications that use Agbale. Defaults MUST be
the secure option; any relaxation MUST be explicit, opt-in and documented. The detailed,
mandatory requirements are listed in the **Security Requirements** section below, and each
of them MUST be covered by dedicated tests.

**Rationale**: a document library processes user data and templates on servers; an insecure
default would be inherited by every user.

### VIII. Actionable and Safe Error Messages

- Every error MUST tell the developer what went wrong and what to do next (e.g. a wrong
  template name lists the available templates; a syntax error gives the template name, line
  and column).
- Errors MUST use the public exception hierarchy (`AgbaleException` and its subtypes).
- Error messages and HTTP responses MUST NEVER expose sensitive information: no stack trace,
  no absolute filesystem path, no secret, no internal class details in HTTP responses.

**Rationale**: clear errors make the 10-minute first PDF possible; safe errors protect
production deployments.

### IX. Built for Francophone Africa, Open to the World

- Defaults target francophone Africa: French locale for dates, FCFA formatting, full support
  for accented characters (embedded free font, working identically in minimal Docker images).
- The locale MUST be configurable; no behaviour may be hard-wired to a single locale or
  currency beyond documented defaults.

**Rationale**: Agbale is born in Lomé for local business needs, and must remain useful
everywhere.

### X. Semantic Versioning and Immutable Releases

- Releases follow Semantic Versioning (`MAJOR.MINOR.PATCH`). Before 1.0.0, minor versions may
  change the API, and every such change MUST be documented in the CHANGELOG.
- A version published to Maven Central is permanent: a release is only made after full
  validation (green CI, tests, coverage, Javadoc, security scans, manual demo check).
- Every release MUST have a `CHANGELOG.md` entry (Keep a Changelog format).

**Rationale**: Maven Central does not allow deleting or overwriting a release.

## Security Requirements

These requirements are mandatory from V1 onwards and are part of Principle VII:

1. **Template names are validated**: no path traversal (`../`, absolute paths, encoded
   variants); resolution is confined to the configured templates location.
2. **No remote resource loading by default**: images, CSS and fonts come only from the
   classpath or Data URIs. `http`, `https`, `file` and any other scheme are refused unless
   explicitly allowed through an allow-list (SSRF protection).
3. **Secure XML/HTML parsing**: external entities and DTD loading are disabled (XXE
   protection).
4. **Automatic escaping**: template variables are HTML-escaped by default; raw output is
   only possible through an explicit construct.
5. **Sandboxed template engine**: Pebble runs in strict and safe mode; templates cannot call
   arbitrary methods or reach sensitive Java classes (`Class`, `ClassLoader`, `System`,
   `Runtime`, reflection…).
6. **Resource limits**: configurable limits on template size, number of pages and rendering
   time, with safe defaults, to prevent resource abuse.
7. **Safe HTTP errors**: the Spring Boot starter returns clean JSON errors without stack
   traces or internal paths.
8. **No secrets in the repository**: GPG keys and tokens live only in GitHub Actions secrets.
9. **Automated scanning**: Dependabot, CodeQL and OWASP Dependency-Check run in CI; the build
   fails on any critical vulnerability.
10. **Dependency licences**: every dependency MUST have a licence compatible with Apache 2.0;
    this is verified before a dependency is added.
11. **Vulnerability disclosure**: `SECURITY.md` explains how to report a vulnerability
    privately.

## Technology Constraints and Development Workflow

### Technology constraints

- Java, compiled with `--release 17` (development on JDK 21 allowed); Maven multi-module.
- `groupId`: `io.github.juste120`; root package: `io.github.juste120.agbale`.
- Template engine: Pebble. HTML→PDF rendering: OpenHTMLtoPDF community edition
  (`io.github.openhtmltopdf`, PDFBox based).
- Spring Boot: current stable line, with the exact compatibility range documented in an ADR.
- Licence: Apache 2.0. Publication through the Maven Central Portal (GPG signature, sources
  and Javadoc JARs, complete POM).
- Every dependency is pinned to its latest stable version, verified at planning time.

### Development workflow

- **Spec-driven development with Spec Kit**: constitution → specify → clarify → plan →
  tasks → analyze → implement. No step is skipped, and the maintainer explicitly approves
  each step before the next one starts. Implementation starts only after the maintainer's
  explicit go-ahead.
- **Sources of truth**, in order of precedence: constitution > spec > plan > tasks.
- **Version scope discipline**: only the currently specified version is implemented. A
  feature belonging to a future version (see `ROADMAP.md`) MUST NOT be implemented without
  an explicit request from the maintainer and its own specification.
- **Commits**: Conventional Commits (`feat:`, `fix:`, `docs:`, `test:`, `refactor:`,
  `build:`, `ci:`, `chore:`).
- **Pull requests**: every PR passes CI and is reviewed against this constitution.
- **Learning-oriented collaboration**: AI assistants working on this repository MUST explain
  every non-trivial technical choice to the maintainer in French (the maintainer is learning
  while building and preparing the OCP Java 21 certification). Code and documentation stay in
  English. These rules are captured in `CLAUDE.md`.

## Governance

- This constitution supersedes all other practices, specifications, plans and tasks. When a
  lower-level artifact conflicts with it, the artifact MUST be corrected, not the reverse.
- **Amendments** are proposed through a pull request that updates this file, explains the
  reason, and includes a Sync Impact Report. Only the maintainer (Juste120) can ratify an
  amendment. Significant amendments are also recorded as an ADR.
- **Versioning of this constitution**: MAJOR for removed or redefined principles, MINOR for
  added principles or materially expanded guidance, PATCH for clarifications and wording.
- **Compliance review**: every plan contains a "Constitution Check" gate; every PR review and
  every `/speckit-analyze` run verifies compliance. Any justified deviation MUST be written
  down in the plan's "Complexity Tracking" section and approved by the maintainer.

**Version**: 1.0.0 | **Ratified**: 2026-11-24 | **Last Amended**: 2026-11-24
