# 0007. Template sandbox through a safe data model

- Status: Accepted
- Date: 2026-11-25

## Context

Template engines that let templates call Java methods are a classic source of remote code
execution (`{{ x.getClass().forName(...) }}`). Data passed by users can be any object, including
JPA entities with lazy proxies and cycles.

## Decision

Two independent barriers:

1. `TemplateModelMapper` converts the data into an **immutable tree of safe values** before
   rendering: maps, lists, `String`, `Boolean`, JDK numbers, `java.time` values, `null`; enums
   become their name; JDK classes are never introspected (converted to strings). Cycles, depth
   (> 32) and size (> 100 000 values) are rejected with clear errors.
2. Pebble runs with a **deny-all `MethodAccessValidator`**, `strictVariables(true)`,
   auto-escaping and `allowOverrideCoreOperators(false)`. Map keys and list indexes are read by
   Pebble's resolvers without calling methods.

## Consequences

- Templates cannot call methods, even harmless ones (`{{ name.toUpperCase() }}` → use
  `{{ name | upper }}`). Computations belong in the response DTO — which also keeps templates
  simple.
- Undefined variables fail fast, catching typos.
- Self- or mutually-recursive layouts (stack overflow) are turned into a clear error.
