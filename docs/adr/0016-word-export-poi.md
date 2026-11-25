# 0016. Word export with Apache POI XWPF in `docmint-word`

- Status: Accepted
- Date: 2026-11-25

## Context

V4 asks for `.docx` export of templates. Options: docx4j with its XHTML importer (JAXB, many
dependencies), LibreOffice/Pandoc conversion (external process), or Apache POI XWPF (already
used by `docmint-excel`).

## Decision

A new optional module `docmint-word` converts the template's HTML (jsoup) into a Word document
with POI XWPF, supporting the structural elements business documents use (headings, formatting,
lists, tables, images, page breaks, header/footer with `PAGE`/`NUMPAGES` fields). It is a
`PostProcessor` (HTML → DOCX) discovered with `ServiceLoader`. Images are taken only from `data:`
URIs; the HTML renderer inlines template images beforehand, with the usual path rules.

## Consequences

- Visual CSS layout is not reproduced; the Word file is a faithful, editable structure.
- No macro and no field from data: data is always plain text runs.
