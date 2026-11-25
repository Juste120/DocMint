# 0017. Streaming and asynchronous rendering

- Status: Accepted
- Date: 2026-11-25

## Context

Large exports (hundreds of thousands of rows) must not be loaded in memory, and long renders
should not block request threads.

## Decision

- CSV list exports can be streamed: `exportCsv(Stream, OutputStream, CsvSettings)` reads the
  column model from the first row, then converts and writes rows lazily (`TableWriter.writeRows`,
  optional for other formats). PDF, Excel and Word stay in memory (their engines need the whole
  document); `render(request, OutputStream)` avoids an extra copy for callers.
- `renderAsync` uses `CompletableFuture.supplyAsync` on a configurable executor (default: a
  fixed pool of daemon threads, one per processor). Virtual threads are not used because the
  build targets Java 17.
- In Spring, `Stream` results of `@Report(format = CSV)` are streamed, and `CompletableFuture`
  results go through Spring MVC's asynchronous support before reaching the `@Report` handler.

## Consequences

The row limit does not apply to streaming (bounded by the caller's stream). Time limits still
apply to each render, including asynchronous ones.
