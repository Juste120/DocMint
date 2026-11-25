# Implementation Plan: DocMint V4 — Word and large volumes

## Decisions
| Topic | Decision | Rejected |
|---|---|---|
| Word | new module `docmint-word` on Apache POI 6 XWPF; own HTML→DOCX converter (jsoup walk); registered as a `PostProcessor` via `ServiceLoader` (builder loads `ServiceLoader<PostProcessor>`) | docx4j + XHTMLImporter (heavy, JAXB); LibreOffice conversion (external process) |
| Pivot | pure-Java `PivotTable` (domain) + Pebble function `pivot` | SQL-like engine |
| Streaming | `render(request, OutputStream)`; CSV streaming writer reusing the column model per row | streaming PDF (OpenHTMLtoPDF needs the whole layout) |
| Async | `CompletableFuture.supplyAsync` on a configurable executor (default: fixed pool of `max(2, cores)` daemon threads); render time limit still applied | virtual threads (Java 21 only; build targets 17) |
| Spring async | let Spring MVC handle `CompletableFuture`; `@Report` handler processes the resolved value | blocking in the handler |

## Constitution Check
II one method/option per feature ✅ · IV SPI reuse (PostProcessor, ServiceLoader) ✅ · V Word in
an optional module, core pipeline unchanged ✅ · VI tests ✅ · VII no macros/fields from data,
injection protection in streaming ✅ · scope guard FR-307 ✅.
