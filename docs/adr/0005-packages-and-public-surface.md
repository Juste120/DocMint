# 0005. Hexagonal packages, public surface, no JPMS in V1

- Status: Accepted
- Date: 2026-11-25

## Context

The constitution imposes packages `api`, `domain`, `application`, `port`, `infrastructure` and
wants only the API to be public. Java has no "visible to sibling packages only" modifier.

## Decision

- **Public API**: `api` (façade, builder, DTO records, exceptions).
- **Public SPI**: `port` and `domain` (needed to plug custom resolvers/renderers — Principle IV).
- **Internal**: `application` and `infrastructure`. Their classes that must be wired across
  packages are `public`, documented as internal in `package-info.java`, excluded from Javadoc,
  without compatibility guarantee. Helpers are package-private (filters, loader, guard...).
- `AgbaleBuilder` is the **composition root**: it instantiates the default adapters.
- `application` returns the `api` contract types (records, exceptions) instead of duplicating
  them; it never references `Agbale`/`AgbaleBuilder`.
- **No `module-info.java` in V1**: Pebble and OpenHTMLtoPDF are automatic modules, which makes
  strict modules brittle for users; almost all users run on the class path. The JAR declares
  `Automatic-Module-Name: io.github.juste120.agbale`. Revisit before 1.0.0.

## Consequences

Rules are enforced by ArchUnit rather than by the compiler. These dependencies are listed in
the plan's Complexity Tracking.
