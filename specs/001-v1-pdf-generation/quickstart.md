# Quickstart — validation scenario for Agbale V1

This is the "first PDF" path that the README reproduces; it doubles as the manual acceptance
check before a release (SC-001).

## 1. Plain Java

```xml
<dependency>
  <groupId>io.github.juste120</groupId>
  <artifactId>agbale-core</artifactId>
  <version>0.1.0</version>
</dependency>
```

`src/main/resources/reports/invoice.html`:

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

```java
record Customer(String name) {}
record Line(String label, BigDecimal amount) {}
record Invoice(String number, Customer customer, List<Line> lines, LocalDate issuedOn) {}

byte[] pdf = Agbale.createDefault().pdf("invoice", new Invoice("F-001",
    new Customer("Kossi Mensah"), List.of(new Line("Ciment", new BigDecimal("125000"))),
    LocalDate.of(2026, 11, 24)));
Files.write(Path.of("invoice.pdf"), pdf);
```

Expected: `invoice.pdf` (A4, "Page 1 / 1" footer) containing `Kossi Mensah`, `125 000 FCFA`,
`24 novembre 2026`.

## 2. Spring Boot

```xml
<dependency>
  <groupId>io.github.juste120</groupId>
  <artifactId>agbale-spring-boot-starter</artifactId>
  <version>0.1.0</version>
</dependency>
```

```java
@RestController
class InvoiceController {
  @GetMapping("/invoices/{id}/pdf")
  @Report(value = "invoice", filename = "facture-{number}.pdf", disposition = Disposition.ATTACHMENT)
  InvoiceResponse invoice(@PathVariable long id) { return service.find(id); }
}
```

`curl -OJ http://localhost:8080/invoices/1/pdf` → `facture-F-001.pdf`.

## 3. Angular

```ts
this.http.get(`/api/invoices/${id}/pdf`, { responseType: 'blob', observe: 'response' })
```

With CORS, expose the header: `allowedHeaders`/`exposedHeaders("Content-Disposition")`.

## 4. Repository checks

```bash
./mvnw verify            # tests, coverage, SpotBugs, Javadoc, architecture rules
./mvnw -pl agbale-examples/demo-api spring-boot:run
```
