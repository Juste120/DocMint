# Agbale

**Business documents from HTML, in one line of Java.**

[![CI](https://github.com/Juste120/agbale/actions/workflows/ci.yml/badge.svg)](https://github.com/Juste120/agbale/actions/workflows/ci.yml)
[![Coverage](https://codecov.io/gh/Juste120/agbale/graph/badge.svg)](https://codecov.io/gh/Juste120/agbale)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.juste120/agbale-core)](https://central.sonatype.com/artifact/io.github.juste120/agbale-core)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

Agbale (*Agbalẽ*: "paper, document" in Ewe, a language of Togo) generates invoices, receipts,
statements, attestations and certificates as PDF from plain HTML/CSS templates. No proprietary
designer, no XML report format, no mandatory configuration: a simple alternative to
JasperReports.

```java
byte[] pdf = agbale.pdf("invoice", invoice);
```

- **Plain HTML/CSS templates** with [Pebble](https://pebbletemplates.io) syntax: variables,
  loops, conditions, layouts. Open them in any IDE or browser.
- **Spring Boot starter**: add `@Report("invoice")` to a controller method and it returns a PDF.
- **Business filters**: `fcfa`, `money`, `sum`, `date` (French by default, any locale).
- **Print-ready by default**: A4, margins, "Page X / Y", page breaks, table headers repeated on
  every page, embedded font with accents and West African letters (works the same in Docker).
- **Secure by default**: no path traversal, no remote resources (SSRF), no XXE, auto-escaping,
  sandboxed templates, size/page/time limits, clean JSON errors.

---

## Installation

Java 17 or later. Spring Boot 4.x for the starter.

**Spring Boot** — `pom.xml`:

```xml
<dependency>
  <groupId>io.github.juste120</groupId>
  <artifactId>agbale-spring-boot-starter</artifactId>
  <version>0.1.0</version>
</dependency>
```

**Plain Java** — use `agbale-core` instead. Several Agbale modules? Import `agbale-bom`.

Gradle: `implementation("io.github.juste120:agbale-spring-boot-starter:0.1.0")`.

## Your first PDF in 5 minutes

**1. Write a template** — `src/main/resources/reports/invoice.html`:

```html
<!DOCTYPE html>
<html lang="fr">
<head><meta charset="UTF-8"><title>Facture {{ number }}</title></head>
<body>
  <h1>Facture {{ number }}</h1>
  <p>Client : {{ customer.name }}</p>
  <table>
    <thead><tr><th>Désignation</th><th>Montant</th></tr></thead>
    <tbody>
      {% for line in lines %}
      <tr><td>{{ line.label }}</td><td>{{ line.amount | fcfa }}</td></tr>
      {% endfor %}
    </tbody>
  </table>
  <p>Total : {{ lines | sum("amount") | fcfa }} — {{ issuedOn | date }}</p>
</body>
</html>
```

**2. Pass your data** — a record, an object with getters, or a `Map`:

```java
record Customer(String name) {}
record Line(String label, BigDecimal amount) {}
record Invoice(String number, Customer customer, List<Line> lines, LocalDate issuedOn) {}

var invoice = new Invoice("F-001", new Customer("Kossi Mensah"),
    List.of(new Line("Ciment", new BigDecimal("125000"))), LocalDate.of(2026, 11, 24));

byte[] pdf = Agbale.createDefault().pdf("invoice", invoice);
Files.write(Path.of("invoice.pdf"), pdf);
```

You get an A4 PDF with `Kossi Mensah`, `125 000 FCFA`, `24 novembre 2026` and "Page 1 / 1".

## Spring Boot + Angular

**API** — the starter configures an `Agbale` bean. One annotation turns a method returning a
DTO into a PDF endpoint:

```java
@RestController
@RequestMapping("/api/invoices")
class InvoiceController {

  @GetMapping("/{id}/pdf")
  @Report(value = "invoice", filename = "invoice-{number}.pdf", disposition = Disposition.ATTACHMENT)
  InvoiceResponse pdf(@PathVariable Long id) {
    return invoiceService.findById(id);   // a response DTO, never a JPA entity
  }
}
```

Anywhere else, inject it: `byte[] pdf = agbale.pdf("invoice", data);`

**Front end** — a PDF is binary, so ask for a `blob`, and read the file name from the header:

```ts
this.http.get(`/api/invoices/${id}/pdf`, { responseType: 'blob', observe: 'response' })
  .subscribe(response => {
    const url = URL.createObjectURL(response.body!);
    window.open(url);                           // "View"; or use <a download> to save
  });
```

See [`agbale-examples/demo-api`](agbale-examples/demo-api) and
[`agbale-examples/demo-angular`](agbale-examples/demo-angular) for a complete example
(invoices, receipts, attestations).

## Filters

| Filter | Example | Output |
|---|---|---|
| `fcfa` | `{{ 1250000 \| fcfa }}` | `1 250 000 FCFA` (no decimals, rounded half-up, XOF and XAF) |
| `money` | `{{ price \| money("EUR") }}` | `1 234,50 €` (locale of the render; `money("USD", "en-US")`) |
| `sum` | `{{ lines \| sum("amount") }}` | exact decimal sum; nested properties: `sum("price.total")` |
| `date` | `{{ issuedOn \| date }}` | `24 novembre 2026`; `date("dd/MM/yyyy")`, `date("d MMMM yyyy", "en")` |
| `raw` | `{{ html \| raw }}` | disables HTML escaping — only for trusted content |

All [Pebble](https://pebbletemplates.io/wiki/) tags and filters are available (`for`, `if`,
`extends`, `block`, `include`, `upper`, `default`, `numberformat`, …).

## Configuration (all optional)

| Property | Default | Description |
|---|---|---|
| `agbale.templates-path` | `reports` | Class path folder of the templates |
| `agbale.cache.enabled` | `true` | Cache compiled templates (disable while editing templates) |
| `agbale.locale` | `fr` | Locale of `date` and `money` |
| `agbale.fonts[0].family` / `.location` | — | Extra TrueType fonts from the class path |
| `agbale.limits.max-template-size` | `1MB` | Maximum size of each template |
| `agbale.limits.max-pages` | `500` | Maximum pages per document |
| `agbale.limits.max-render-time` | `30s` | Maximum render duration |
| `agbale.resources.allowed-hosts` | empty | HTTPS hosts allowed for remote images/CSS |
| `agbale.resources.missing` | `fail` | `fail` or `warn` when an image/CSS file is missing |
| `agbale.web.problem-details.enabled` | `true` | RFC 9457 JSON errors for Agbale failures |

Without Spring: `Agbale.builder().templatesPath("documents").locale(Locale.ENGLISH).build()`.

## FAQ

**Accents or letters like ɖ, ŋ, ɔ appear as squares?** They will not with Agbale: Noto Sans is
embedded and used for every font family, so output is identical on your laptop and in a
`eclipse-temurin:21-jre` Docker image without fonts. For emoji, CJK or symbols such as arrows,
register an extra font (`agbale.fonts`).

**My logo does not appear.** Put it in the templates folder (`reports/images/logo.png`) and use
`<img src="images/logo.png">` (relative to the template) or `/images/logo.png` (from the
templates root). `data:` URIs also work. Remote URLs are refused unless allow-listed; Agbale
fails with `AGBALE_RESOURCE_MISSING` or `AGBALE_RESOURCE_REFUSED` rather than silently dropping
the image.

**Angular cannot read the file name.** With CORS, the browser hides `Content-Disposition` unless
the API exposes it: `registry.addMapping("/api/**").exposedHeaders("Content-Disposition")`.

**The error response is a Blob in Angular.** With `responseType: 'blob'`, errors are blobs too:
`await error.error.text()` then `JSON.parse(...)` gives the Problem Details (`code`, `detail`).

**Can templates call Java methods?** No, by design: templates only see data (maps, lists, simple
values). Compute values in your response DTO.

**Is the LGPL dependency a problem?** OpenHTMLtoPDF is LGPL-2.1 and used as an unmodified,
separate library, which is compatible with Agbale's Apache 2.0 licence and with closed-source
applications. See [ADR-0003](docs/adr/0003-openhtmltopdf-and-jsoup.md).

## Documentation

- [User guide](docs/user-guide.md) — templates, filters, layouts, errors, security, limits
- [Architecture](docs/architecture.md) and [decision records](docs/adr/)
- [Contributing](CONTRIBUTING.md) · [Security policy](SECURITY.md) · [Changelog](CHANGELOG.md)

## Roadmap

| Version | Theme |
|---|---|
| **0.1.0** | The PDF in one line (this release) |
| 0.2.0 | Professional documents: headers/footers, watermark, QR and barcodes, amount in words, password protection, PNG preview, Angular helper |
| 0.3.0 | Charts and Excel: `<chart>` (SVG), Excel and CSV export |
| 0.4.0 | Word and large volumes: `.docx`, pivot tables, streaming, async generation |
| 0.5.0 | Official documents: e-signature, PDF/A, accessibility |
| 1.0.0 | Stable API, complete documentation, migration guide from JasperReports |

Details in [ROADMAP.md](ROADMAP.md).

---

## En français

Agbale génère des documents métier (factures, reçus, relevés, attestations, certificats) en PDF à
partir de templates HTML/CSS, **en une ligne de Java**. Il n'y a ni éditeur propriétaire, ni
format XML, ni configuration obligatoire.

- **Spring Boot** : ajoutez `agbale-spring-boot-starter`, mettez vos templates dans
  `src/main/resources/reports/`, puis annotez une méthode de contrôleur avec
  `@Report("facture")`. Elle renvoie un DTO et le client reçoit un PDF.
- **Java pur** : `byte[] pdf = Agbale.createDefault().pdf("facture", donnees);`
- **Pensé pour l'Afrique francophone** : le filtre `fcfa` affiche `1 250 000 FCFA`, les dates
  sont en français (`24 novembre 2026`), et la police intégrée gère les accents et les lettres
  éwé (ɖ, ɛ, ƒ, ɣ, ŋ, ɔ, ʋ), y compris dans Docker.
- **Sécurisé par défaut** : aucun accès aux fichiers ni au réseau depuis un template, données
  échappées automatiquement, limites de taille, de pages et de temps, et erreurs JSON sans
  détails internes.

Questions, idées, contributions : ouvrez une *issue* (en français ou en anglais) et lisez
[CONTRIBUTING.md](CONTRIBUTING.md).

## License

[Apache License 2.0](LICENSE). Bundled Noto Sans font: SIL Open Font License 1.1 (see
[NOTICE](NOTICE)).
