# DocMint — Angular demo

A minimal Angular 22 application that calls the [demo API](../demo-api) and shows how to
display or download PDFs generated with DocMint.

## Run

Requirements: Node.js 22.22+ or 24.15+.

```bash
# terminal 1 — from the repository root
./mvnw -pl docmint-examples/demo-api -am spring-boot:run

# terminal 2
cd docmint-examples/demo-angular
npm install
npm start
```

Open http://localhost:4200 and click **Voir** (view) or **Télécharger** (download).

## What to look at

- [`report.service.ts`](src/app/report.service.ts): `responseType: 'blob'` and
  `observe: 'response'`, file name read from `Content-Disposition`.
- [`documents.component.ts`](src/app/documents.component.ts): reading RFC 9457 errors, which
  arrive as a `Blob` because of `responseType: 'blob'`.
- The API must expose `Content-Disposition` with CORS (`exposedHeaders`), otherwise the browser
  hides it from JavaScript.
