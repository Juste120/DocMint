# 0002. Pebble as template engine

- Status: Accepted
- Date: 2026-11-25

## Context

Templates must be standard HTML/CSS editable in any IDE (Principle III), support variables,
loops, conditions and layout inheritance, and run in a safe mode (Principle VII).

## Decision

Use Pebble 4.x (`io.pebbletemplates:pebble`, BSD-3-Clause): Twig/Jinja-like syntax, `extends` /
`block` / `include`, filters, auto-escaping on by default, `strictVariables`,
`maxRenderedSize`, pluggable `Loader`, `PebbleCache` and `MethodAccessValidator`. Our own
`DocMintExtension` adds the business filters and replaces Pebble's `date` filter.

## Consequences

- Pebble only appears in `infrastructure.pebble`; another engine could be added as another
  `TemplateEngine` adapter.
- Template authors learn a small, widely known syntax.
- Runtime dependencies: `unbescape` (Apache 2.0), `slf4j-api` (MIT).

## Alternatives considered

- **Thymeleaf**: natural templates, but heavier and verbose for tables/loops.
- **FreeMarker**: powerful, but its default object wrapper exposes Java methods; harder to
  sandbox.
- **Mustache**: no layout inheritance, no filters.
