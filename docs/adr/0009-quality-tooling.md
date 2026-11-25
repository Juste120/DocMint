# 0009. Quality tooling: Spotless + SpotBugs + JaCoCo + ArchUnit

- Status: Accepted
- Date: 2026-11-25

## Context

The constitution requires automatic formatting and static analysis, ≥ 80 % coverage on
`docmint-core`, Javadoc on the public API, and architecture rules checked by the build.

## Decision

- **Spotless** with **google-java-format 1.35.0**: the formatter is the single source of truth
  for style, so **Checkstyle is not used** (overlapping rules would conflict). Version 1.36+
  needs `commonmark`, which Spotless 3.12 does not put on the formatter class path (format
  failures on Javadoc); upgrade when fixed. google-java-format needs JDK 21+ and access to
  `jdk.compiler` internals (`.mvn/jvm.config`): the check runs automatically on JDK 21+ (profile
  `format`), i.e. locally and in the JDK 21 CI job.
- **SpotBugs** (effort Max, threshold Medium) on `verify`; exclusions are justified in
  `config/spotbugs-exclude.xml`.
- **JaCoCo** check: line coverage ≥ 80 % on `docmint-core` (≈ 90 % at 0.1.0).
- **Javadoc** built on `verify` (doclint) for the public packages.
- **ArchUnit** rules as plain JUnit tests (`ArchitectureTest`).
- Security: **CodeQL**, **OWASP Dependency-Check** (`failBuildOnCVSS=9`), **gitleaks**,
  **Dependabot**.

## Consequences

`./mvnw verify` is the single local quality gate; CI runs it on JDK 17 and 21.
