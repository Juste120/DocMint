package io.github.juste120.docmint.infrastructure.pdf;

import com.openhtmltopdf.extend.FSStream;
import com.openhtmltopdf.extend.FSStreamFactory;
import com.openhtmltopdf.extend.FSUriResolver;
import com.openhtmltopdf.outputdevice.helper.ExternalResourceType;
import com.openhtmltopdf.util.Diagnostic;
import io.github.juste120.docmint.domain.ResourceAccessPolicy;
import io.github.juste120.docmint.domain.TemplateName;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Controls every resource (image, stylesheet, font) a document loads during one render.
 *
 * <ul>
 *   <li>Relative references ({@code images/logo.png}) resolve against the folder of the rendered
 *       template, and references starting with {@code /} against the templates root, under the
 *       private scheme {@value #SCHEME}; they are read from the class path only. A reference that
 *       leaves the templates root is refused.
 *   <li>{@code data:} URIs are allowed.
 *   <li>Remote {@code https} resources are allowed only for allow-listed hosts; every other scheme
 *       ({@code http}, {@code file}, {@code jar}, ...) is refused. This prevents SSRF.
 * </ul>
 *
 * <p>The engine silently skips refused or unreadable resources, so the guard records them; the
 * renderer then fails or warns. One instance is used for exactly one render (not thread-safe).
 */
final class ResourceGuard {

  static final String SCHEME = "docmint-classpath";

  private static final Set<String> MISSING_DIAGNOSTICS =
      Set.of(
          "LOAD_UNRECOGNIZED_IMAGE_FORMAT_FOR_URI",
          "EXCEPTION_CANT_READ_IMAGE_FILE_FOR_URI",
          "EXCEPTION_CANT_READ_IMAGE_FILE_FOR_URI_NOT_FOUND",
          "LOAD_UNABLE_TO_LOAD_CSS_FROM_URI",
          "EXCEPTION_ITEM_AT_URI_NOT_FOUND",
          "EXCEPTION_IO_PROBLEM_FOR_URI",
          "EXCEPTION_MALFORMED_URL",
          "EXCEPTION_FAILED_TO_LOAD_BACKGROUND_IMAGE_AT_URI",
          "EXCEPTION_COULD_NOT_LOAD_FONT_FACE");

  private final String root;
  private final ClassLoader classLoader;
  private final ResourceAccessPolicy policy;
  private final String baseUri;
  private final Set<String> refused = new LinkedHashSet<>();
  private final Set<String> missing = new LinkedHashSet<>();

  ResourceGuard(
      String root, TemplateName template, ClassLoader classLoader, ResourceAccessPolicy policy) {
    this.root = root;
    this.classLoader = classLoader;
    this.policy = policy;
    this.baseUri = SCHEME + ":/" + root + "/" + template.folder();
  }

  String baseUri() {
    return baseUri;
  }

  FSUriResolver uriResolver() {
    return (base, uri) -> {
      if (uri == null || uri.isBlank()) {
        return null;
      }
      String trimmed = uri.strip();
      if (hasScheme(trimmed)) {
        return trimmed; // absolute: decided by the access control
      }
      if (trimmed.startsWith("//")) {
        refused.add(display(trimmed)); // protocol-relative URL: a remote host
        return null;
      }
      if (trimmed.startsWith("/")) {
        trimmed = trimmed.substring(1); // "/images/logo.png" = from the templates root
        base = SCHEME + ":/" + root + "/";
      }
      try {
        URI relative = new URI(trimmed.replace(" ", "%20"));
        return new URI(base == null ? baseUri : base).resolve(relative).toString();
      } catch (URISyntaxException | IllegalArgumentException e) {
        missing.add(display(trimmed));
        return null;
      }
    };
  }

  boolean allows(String uri, ExternalResourceType type) {
    if (uri == null) {
      return false;
    }
    String lower = uri.toLowerCase(Locale.ROOT);
    if (lower.startsWith("data:")) {
      return true;
    }
    if (lower.startsWith(SCHEME + ":")) {
      if (classpathPath(uri) == null) {
        refused.add(display(uri));
        return false;
      }
      return true;
    }
    if (policy.allowsRemote(uri)) {
      return true;
    }
    refused.add(display(uri));
    return false;
  }

  FSStreamFactory classpathStreams() {
    return uri -> {
      String path = classpathPath(uri);
      if (path == null) {
        refused.add(display(uri));
        return null;
      }
      InputStream in = classLoader.getResourceAsStream(path);
      if (in == null) {
        missing.add(display(uri));
      }
      // The engine expects a stream object; a null input stream means "not found".
      return new FSStream() {
        @Override
        public InputStream getStream() {
          return in;
        }

        @Override
        public Reader getReader() {
          return in == null ? null : new InputStreamReader(in, StandardCharsets.UTF_8);
        }
      };
    };
  }

  Consumer<Diagnostic> diagnostics() {
    return diagnostic -> {
      if (diagnostic.getLogMessageId() == null) {
        return;
      }
      String id = diagnostic.getLogMessageId().getEnum().name();
      if (MISSING_DIAGNOSTICS.contains(id)) {
        Object[] args = diagnostic.getArgs();
        String resource =
            args != null && args.length > 0 && args[0] != null ? args[0].toString() : id;
        if (!refused.contains(display(resource))) {
          missing.add(display(resource));
        }
      }
    };
  }

  /**
   * Returns the class path location of a private-scheme URI, or {@code null} if it escapes the
   * root.
   */
  String classpathPath(String uri) {
    try {
      URI parsed = new URI(uri);
      if (!SCHEME.equalsIgnoreCase(parsed.getScheme()) || parsed.getPath() == null) {
        return null;
      }
      String path = parsed.normalize().getPath();
      String prefix = "/" + root + "/";
      if (!path.startsWith(prefix) || path.contains("..") || path.contains("\\")) {
        return null;
      }
      String relative = path.substring(prefix.length());
      for (String segment : relative.split("/", -1)) {
        if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
          return null;
        }
      }
      return root + "/" + relative;
    } catch (URISyntaxException e) {
      return null;
    }
  }

  List<String> refused() {
    return new ArrayList<>(refused);
  }

  List<String> missing() {
    List<String> result = new ArrayList<>(missing);
    result.removeAll(refused);
    return result;
  }

  private static boolean hasScheme(String uri) {
    int colon = uri.indexOf(':');
    if (colon <= 0) {
      return false;
    }
    for (int i = 0; i < colon; i++) {
      char c = uri.charAt(i);
      if (!(Character.isLetterOrDigit(c) || c == '+' || c == '-' || c == '.')) {
        return false;
      }
    }
    return true;
  }

  /** Short, safe description of a resource for error messages (never an absolute file path). */
  private String display(String uri) {
    String value = uri;
    String prefix = SCHEME + ":/" + root + "/";
    if (value.startsWith(prefix)) {
      value = value.substring(prefix.length());
    } else if (value.regionMatches(true, 0, "data:", 0, 5)) {
      int comma = value.indexOf(',');
      value = (comma > 0 ? value.substring(0, Math.min(comma, 40)) : "data:") + ",…";
    } else if (value.regionMatches(true, 0, "file:", 0, 5)) {
      value = "file:…";
    }
    return value.length() > 120 ? value.substring(0, 120) + "…" : value;
  }
}
