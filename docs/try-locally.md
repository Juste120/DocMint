# Try DocMint in another project before it is published

Until DocMint is on Maven Central and npm, install it on your machine and use the
`-SNAPSHOT` version.

## 1. Install the Java artifacts in your local Maven repository

```bash
cd /path/to/DocMint
./mvnw install -DskipTests -DskipExamples
```

This puts `docmint-core`, `docmint-spring-boot-starter` and `docmint-bom` (version
`0.2.0-SNAPSHOT`) in `~/.m2/repository/io/github/juste120/`.

## 2. Use it in a new Spring Boot 4 project

```xml
<dependency>
  <groupId>io.github.juste120</groupId>
  <artifactId>docmint-spring-boot-starter</artifactId>
  <version>0.2.0-SNAPSHOT</version>
</dependency>
```

Put a template in `src/main/resources/reports/facture.html` and annotate a controller method:

```java
@GetMapping("/factures/{numero}")
@Report(value = "facture", filename = "facture-{numero}.pdf")
public Facture facture(@PathVariable String numero) { … }
```

Plain Java (no Spring): depend on `docmint-core` and call
`DocMint.createDefault().pdf("facture", data)`.

## 3. Use the Angular library in a front end

```bash
cd /path/to/DocMint/docmint-angular
npm ci && npm run build
cd dist/docmint-angular && npm pack          # creates juste120-docmint-angular-0.2.0.tgz

cd /path/to/your-angular-app
npm install /path/to/DocMint/docmint-angular/dist/docmint-angular/juste120-docmint-angular-0.2.0.tgz
```

Then `inject(DocMintClient)` (with `provideHttpClient()`), and enable CORS with
`exposedHeaders("Content-Disposition")` on the API.

## 4. After changing DocMint

Run step 1 again, then rebuild your project (`./mvnw -U package` forces the new snapshot).
