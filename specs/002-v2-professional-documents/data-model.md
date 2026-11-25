# Data Model — DocMint V2 (additions to V1)

## Domain (`…docmint.domain`)

| Type | Kind | Fields / rules |
|---|---|---|
| `OutputFormat.PNG` | enum constant | `image/png`, `png`, `baseFormat() = PDF` (PDF → `PDF`) |
| `Watermark` | record | `text` or `imageLocation` (exactly one), `angle` (−90…90, default 45), `opacity` (0.05…1, default 0.15), `color` (`#RRGGBB`, default `#808080`), `fontSize` (8…200 pt, default 0 = auto-fit), `layer` (`BEHIND` default, `OVER`); factories `text(String)`, `image(String)`, `with…` copies |
| `Protection` | final class | `openPassword` (`char[]`, optional), `ownerPassword` (`char[]`, optional → random), `permissions` (`Set<Permission>`); factories `openPassword(..)`, `permissions(..)`, `withOwnerPassword(..)`; `toString()` redacted; `clear()` wipes arrays |
| `Permission` | enum | `PRINT`, `PRINT_HIGH_QUALITY`, `COPY`, `MODIFY`, `ANNOTATE`, `FILL_FORMS`, `EXTRACT_FOR_ACCESSIBILITY`, `ASSEMBLE` |
| `PreviewSettings` | record | `page` (1-based, default 1), `dpi` (36…300, default 96) |
| `DocumentOptions` | record | `watermark`, `protection`, `preview` (nullable each); `none()` |
| `ResourceLimits` | record (+1 component) | `maxImagePixels` (default 25 000 000); V1 3-arg constructor kept |

## API (`…docmint.api`)

- `RenderOptions` gains `watermark`, `protection`, `preview` + `with…` methods; the V1
  2-argument constructor is kept (source compatibility).
- `DocMint.png(name, data)`; `DocMintBuilder.defaultWatermark(Watermark)`.
- `LimitExceededException.Limit.IMAGE_SIZE`.
- New exceptions: `CodeGenerationException` (`DOCMINT_CODE_INVALID`),
  `InvalidOptionsException` (`DOCMINT_PREVIEW_PAGE_OUT_OF_RANGE`, `DOCMINT_PROTECTION_INVALID`,
  `DOCMINT_WATERMARK_INVALID`).

## Ports (`…docmint.port`)

- `PostProcessor { boolean appliesTo(OutputFormat current, OutputFormat requested,
  DocumentOptions options); RenderedDocument process(RenderedDocument, PostProcessContext); }`
- `PostProcessContext(TemplateName, OutputFormat requested, DocumentOptions, ResourceLimits)`.
- `RenderContext` gains `options` (V1 3-arg constructor kept).

## Starter

- `@Report(format = OutputFormat.PDF)`.
- `ReportContent.of(data).withWatermark(..).withProtection(..).withFileName(..).withPreview(..)`
  — a method may return it instead of the DTO.
- Properties `docmint.watermark.{text,angle,opacity,color}`, `docmint.limits.max-image-pixels`.
