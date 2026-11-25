# Security Policy

DocMint renders documents inside other people's applications, so security is a core principle
of the project (see the [constitution](.specify/memory/constitution.md), Principle VII).

## Supported versions

| Version | Supported |
|---|---|
| 0.1.x | ✅ |

Before 1.0.0, only the latest minor version receives security fixes.

## Reporting a vulnerability

**Do not open a public issue.** Report privately through
[GitHub Security Advisories](https://github.com/Juste120/DocMint/security/advisories/new)
("Report a vulnerability"). Only the maintainers can see the report.

Please include the affected version, a description, a minimal template/code to reproduce, and
the impact you observed. You will receive an acknowledgement within 5 days. Once a fix is
released, the advisory is published with credit to you (unless you prefer to stay anonymous).

## What DocMint protects against by default

- Path traversal in template names, `extends`/`include`, and resource paths.
- Server-side request forgery: no `http`, `https`, `file` or other remote resource unless an
  HTTPS host is explicitly allow-listed.
- XML external entities (HTML is parsed with jsoup, which never resolves entities or DTDs).
- HTML injection: template variables are escaped; unescaped output requires the `raw` filter.
- Template sandbox escape: templates only see maps, lists and simple values, and every method
  call is denied.
- Resource exhaustion: limits on template size, rendered size, page count and render time.
- Information disclosure: error messages and HTTP error bodies never contain stack traces,
  absolute paths or data values.

Template **files** are considered trusted code written by the application's developers. Do not
load templates from end-user input.

## Automated checks

Every build runs CodeQL, OWASP Dependency-Check (fails on CVSS ≥ 9), a secret scan, and
Dependabot keeps dependencies up to date.
