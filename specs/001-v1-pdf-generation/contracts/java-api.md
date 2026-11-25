# Contract — Public Java API (`agbale-core`)

Package `io.github.juste120.agbale.api`. Everything below is public API, covered by Javadoc and
by semantic versioning.

```java
public final class Agbale {
    public static AgbaleBuilder builder();
    public static Agbale createDefault();                         // == builder().build()

    public byte[] pdf(String templateName, Object data);           // the one-liner
    public byte[] pdf(String templateName, Object data, Locale locale);
    public RenderResponse render(RenderRequest request);           // advanced API
    public List<String> availableTemplates();                      // sorted, bounded (≤ 500)
    public void clearCache();
}

public final class AgbaleBuilder {
    public AgbaleBuilder templatesPath(String classpathRoot);      // default "reports"
    public AgbaleBuilder cache(boolean enabled);                   // default true
    public AgbaleBuilder locale(Locale locale);                    // default Locale.FRENCH
    public AgbaleBuilder font(String family, String classpathLocation);
    public AgbaleBuilder font(FontDefinition font);
    public AgbaleBuilder limits(ResourceLimits limits);            // default ResourceLimits.defaults()
    public AgbaleBuilder allowRemoteHost(String host);             // https only, exact or "*.domain"
    public AgbaleBuilder missingResources(MissingResourceMode mode); // default FAIL
    public AgbaleBuilder pageSettings(PageSettings settings);      // default A4 portrait
    public AgbaleBuilder classLoader(ClassLoader classLoader);     // default: thread context CL
    // expert escape hatches (Principle IV)
    public AgbaleBuilder templateResolver(TemplateResolver resolver);
    public AgbaleBuilder renderer(DocumentRenderer renderer);
    public Agbale build();                                         // validates, immutable, thread-safe
}

public record RenderRequest(String templateName, Object data, OutputFormat format, RenderOptions options) {
    public static RenderRequest of(String templateName, Object data);
}
public record RenderOptions(Locale locale, String fileName) { public static RenderOptions defaults(); }
public record RenderResponse(byte[] content, String mediaType, String fileName, int pageCount) { }
```

## SPI (expert extension points)

Package `io.github.juste120.agbale.port`:

```java
public interface TemplateResolver {
    Optional<TemplateSource> resolve(TemplateName name);  // TemplateSource(name, content, location)
    List<String> availableTemplates();
}
public interface TemplateEngine {                        // HTML producer
    String render(TemplateName name, Map<String, Object> model, Locale locale);
    void clearCache();
}
public interface DocumentRenderer {                      // one per OutputFormat (Strategy)
    OutputFormat format();
    RenderedDocument render(String html, RenderContext context);
}
public interface TemplateCache {
    <T> T getOrCompute(String key, Supplier<T> compiler);
    void clear();
}
```

`RenderContext` (record in `port`): template name, locale, base resource path, limits, policy,
missing-resource mode, page settings, fonts.

## Template filters (template contract)

| Filter | Input | Arguments | Output example |
|---|---|---|---|
| `fcfa` | number / numeric string / null | — | `1250000` → `1 250 000 FCFA` (spaces are U+00A0 no-break spaces) |
| `money` | number / null | `currency` (ISO 4217, required), `locale` (optional, e.g. `"en-US"`) | `1234.5\|money("EUR")` → `1 234,50 €` |
| `sum` | list of numbers or of objects/maps / null | `property` (optional, dotted path) | `lines\|sum("amount")` |
| `date` | `LocalDate`, `LocalDateTime`, `OffsetDateTime`, `ZonedDateTime`, `Instant` (UTC), `Date` / null | `pattern` (optional), `locale` (optional) | `→ 24 novembre 2026`; `date("dd/MM/yyyy")` → `24/11/2026` |
| `raw` | any | — | Pebble built-in: disables escaping explicitly |

Note on spaces: Java's French formatting uses U+202F (narrow no-break space) as grouping
separator. `fcfa` and `money` output U+00A0 (no-break space) so that amounts never wrap and
extracted text is predictable; tests normalise to that character.

Variables: root properties at top level; a non-object root under `data` (FR-002).
