# 0004. Embed Noto Sans

- Status: Accepted
- Date: 2026-11-25

## Context

PDF standard fonts (Helvetica, Times) only cover WinAnsi: no `ŋ`, `ɖ`, `ɔ`… Server and Docker
images often have no fonts at all. Accents must work everywhere (Principle IX), including Ewe.

## Decision

Bundle **Noto Sans 2.004** (Regular, Bold, Italic, Bold Italic; SIL OFL 1.1; ~2 MB) in
`agbale-core`, with its licence. Coverage was verified with fontTools: French diacritics, `Œ œ`,
`€`, U+202F, em dash, Ewe letters `Ɖ ɖ Ɛ ɛ Ƒ ƒ Ɣ ɣ Ŋ ŋ Ɔ ɔ Ʋ ʋ` and the combining tilde.
Register it as `Noto Sans`, `sans-serif`, `serif`, `monospace`, **and as a pre-fallback font**,
so unknown families (`Arial` on Linux) never fall back to Times-Roman. Font bytes are read once
and kept in memory.

## Consequences

- The core JAR is ~2 MB bigger.
- Identical output on every machine (tested by extracting French and Ewe text).
- Arrows, emoji and CJK are not covered; users register extra fonts (`agbale.fonts`).
- A test revealed the fallback problem (Arial → Times-Roman); `FALLBACK_PRE` fixes it.

## Alternatives considered

- **DejaVu Sans**: good coverage, older design, larger.
- **System fonts**: not reproducible, absent in Docker.
