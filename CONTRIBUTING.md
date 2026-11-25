# Contributing to DocMint

Thank you for your interest! Contributions of any size are welcome: bug reports, documentation,
templates, tests, code. Issues and discussions can be written in English or French.

## Ground rules

- Read the [constitution](.specify/memory/constitution.md): it is short and every pull request is
  reviewed against it (simplicity, security by default, tests, clean architecture).
- One version at a time: features belong to the version being built (see [ROADMAP](ROADMAP.md)).
  A feature planned for a later version needs a discussion first.
- Be kind: see the [Code of Conduct](CODE_OF_CONDUCT.md).
- Security issues are reported privately: see [SECURITY.md](SECURITY.md).

## Development setup

Requirements: JDK 21 (the code is compiled with `--release 17`; JDK 17 also works), Git.
Maven is provided by the wrapper. Node.js 22.22+ or 24.15+ only for the Angular demo.

```bash
git clone https://github.com/Juste120/DocMint.git
cd docMint
./mvnw verify
```

`verify` runs, for every module:

- unit, integration, security and architecture tests (JUnit 6, AssertJ, ArchUnit, PDFBox);
- JaCoCo — line coverage of `docmint-core` must stay ≥ 80 %;
- SpotBugs;
- Javadoc of the public API;
- Spotless formatting check (google-java-format) — on JDK 21+.

Format the code before committing: `./mvnw spotless:apply`.

Run the demo: `./mvnw -pl docmint-examples/demo-api -am spring-boot:run`, then
`http://localhost:8080/api/invoices/1/pdf`.

## Project layout

| Module | Content |
|---|---|
| `docmint-core` | The engine, pure Java (no Spring). Packages `api`, `domain`, `port`, `application`, `infrastructure` |
| `docmint-spring-boot-starter` | Auto-configuration, `@Report`, error handling |
| `docmint-bom` | Version alignment |
| `docmint-examples/*` | Demos, never published |

Read [docs/architecture.md](docs/architecture.md) before changing the core. Architecture rules are
enforced by `ArchitectureTest`.

## Conventions

- **Language**: code, Javadoc, documentation and commit messages in English.
- **Commits**: [Conventional Commits](https://www.conventionalcommits.org/) —
  `feat:`, `fix:`, `docs:`, `test:`, `refactor:`, `build:`, `ci:`, `chore:`; scope optional
  (`feat(starter): …`).
- **Tests first-class**: every change comes with tests; security-related code gets a test in
  `docmint-core/src/test/java/…/security`.
- **Errors**: throw an `DocMintException` subtype with a message that says what to do next; never
  include absolute paths, stack traces or data values.
- **Decisions**: a significant technical choice gets an ADR in `docs/adr/` (copy the format of an
  existing one).
- **Changelog**: user-visible changes go under `[Unreleased]` in `CHANGELOG.md`.

## Spec-driven development

New features follow [Spec Kit](https://github.com/github/spec-kit): `specify` → `clarify` →
`plan` → `tasks` → `analyze` → `implement`, with artifacts in `specs/`. Sources of truth, in
order: constitution > spec > plan > tasks.

## Pull requests

1. Fork, create a branch from `main`.
2. Make your change with tests; run `./mvnw verify`.
3. Open a pull request using the template; CI must be green.
