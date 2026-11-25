# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this
project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html). Before 1.0.0,
minor versions may change the public API; every such change is listed here.

## [Unreleased]

## [0.1.0] - Not yet released

### Added

- `agbale-core`: PDF generation from HTML/CSS templates in one line —
  `Agbale.createDefault().pdf(name, data)` and the advanced `render(RenderRequest)` API.
- Pebble templates with variables, loops, conditions, includes and layout inheritance; data from
  records, JavaBeans or maps.
- Filters `fcfa`, `money`, `sum` and `date` (French by default, configurable locale).
- Embedded Noto Sans font (French and West African Latin letters), used as fallback font.
- Default print stylesheet: A4, margins, "Page X / Y", `.page-break`, repeated table headers.
- Images from the class path and `data:` URIs; thread-safe template cache.
- Security by default: template name validation, no remote or file resources unless an HTTPS
  host is allow-listed, XXE-proof HTML parsing, auto-escaping, sandboxed templates, limits on
  template size, rendered size, pages and render time.
- Actionable errors with stable codes (`AGBALE_TEMPLATE_NOT_FOUND`, …) and name suggestions.
- `agbale-spring-boot-starter` for Spring Boot 4: auto-configured `Agbale` bean, `agbale.*`
  properties, `@Report` annotation with file name placeholders, RFC 9457 error responses.
- `agbale-bom`.
- Demo Spring Boot API and Angular application (not published).

[Unreleased]: https://github.com/Juste120/agbale/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/Juste120/agbale/releases/tag/v0.1.0
