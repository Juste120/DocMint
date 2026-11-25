# 0013. Angular helper library `@juste120/docmint-angular`

- Status: Accepted
- Date: 2026-11-25

## Context

Every front end calling DocMint endpoints repeats the same code: `responseType: 'blob'`, reading
the file name from `Content-Disposition`, opening/downloading blobs, and reading RFC 9457 errors
that arrive as `Blob`s.

## Decision

- An Angular workspace `docmint-angular/` (outside the Maven build) builds the library with
  ng-packagr (Angular Package Format), published to npm as `@juste120/docmint-angular` with the
  same version as the Java artifacts. Peer dependencies only: Angular and RxJS.
- `DocMintClient` uses constructor injection of `HttpClient`, so it is unit-tested with Vitest
  and a fake client, without TestBed or a browser.
- The demo application consumes the built library (`file:` dependency, `preserveSymlinks`) to
  avoid two copies of Angular.

## Consequences

CI has an Angular job (test + build library, build demo). Publication to npm is manual, like
Maven Central, and needs the maintainer's npm account.
