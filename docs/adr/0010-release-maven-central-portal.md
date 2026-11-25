# 0010. Release through the Maven Central Portal

- Status: Accepted
- Date: 2026-11-25

## Context

Artifacts are published to Maven Central under `io.github.juste120`. A published version can
never be changed or deleted (Principle X). Secrets must never be in the repository.

## Decision

- Single version for all modules with CI-friendly `${revision}` and `flatten-maven-plugin`
  (published POMs are complete and have no parent).
- `central-publishing-maven-plugin` replaces `maven-deploy-plugin`; parent and demo artifacts
  are excluded. Profile `release` adds sources, Javadoc and GPG signatures.
- Release = manual GitHub Actions workflow (`release.yml`) with the version as input: it re-runs
  the whole CI, then deploys with `autoPublish=false` and `waitUntil=validated`. The maintainer
  checks the validated deployment in the Central Portal and clicks **Publish**. The tag
  `v<version>` is pushed by the workflow.
- Secrets (`GPG_PRIVATE_KEY`, `GPG_PASSPHRASE`, `CENTRAL_USERNAME`, `CENTRAL_TOKEN`,
  `NVD_API_KEY`, `CODECOV_TOKEN`) live only in GitHub Actions secrets, in a protected
  `maven-central` environment.

## Consequences

A human validation step remains between a green build and an irreversible publication.
