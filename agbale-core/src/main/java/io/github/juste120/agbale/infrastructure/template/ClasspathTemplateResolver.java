package io.github.juste120.agbale.infrastructure.template;

import io.github.juste120.agbale.api.LimitExceededException;
import io.github.juste120.agbale.api.RenderException;
import io.github.juste120.agbale.domain.TemplateName;
import io.github.juste120.agbale.port.TemplateResolver;
import io.github.juste120.agbale.port.TemplateSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Reads templates from the class path, below a root folder such as {@code reports}.
 *
 * <p>Only validated {@link TemplateName}s are accepted, so a lookup can never leave the root.
 * Files are read with a size limit and decoded as UTF-8.
 */
public final class ClasspathTemplateResolver implements TemplateResolver {

  static final int MAX_LISTED = 500;

  private final String root;
  private final ClassLoader classLoader;
  private final long maxTemplateBytes;

  /**
   * Creates a resolver.
   *
   * @param root the class path folder, e.g. {@code reports}
   * @param classLoader the class loader to read from
   * @param maxTemplateBytes the maximum size of a template file
   */
  public ClasspathTemplateResolver(String root, ClassLoader classLoader, long maxTemplateBytes) {
    this.root = normaliseRoot(root);
    this.classLoader = Objects.requireNonNull(classLoader, "classLoader");
    this.maxTemplateBytes = maxTemplateBytes;
  }

  /**
   * Validates and normalises a class path root folder.
   *
   * @param root the root, e.g. {@code /reports/}
   * @return the normalised root, e.g. {@code reports}
   * @throws IllegalArgumentException if the root is invalid
   */
  public static String normaliseRoot(String root) {
    Objects.requireNonNull(root, "templates path");
    String value = root.strip();
    if (value.startsWith("classpath:")) {
      value = value.substring("classpath:".length());
    }
    while (value.startsWith("/")) {
      value = value.substring(1);
    }
    while (value.endsWith("/")) {
      value = value.substring(0, value.length() - 1);
    }
    if (value.isEmpty() || !TemplateName.isValid(value)) {
      throw new IllegalArgumentException(
          "invalid templates path '" + root + "': use a class path folder such as 'reports'");
    }
    return value;
  }

  @Override
  public Optional<TemplateSource> resolve(TemplateName name) {
    String path = root + "/" + name.path();
    try (InputStream in = classLoader.getResourceAsStream(path)) {
      if (in == null) {
        return Optional.empty();
      }
      return Optional.of(new TemplateSource(name, read(in, name)));
    } catch (IOException e) {
      throw new RenderException("Cannot read template '" + name + "'.", e);
    }
  }

  private String read(InputStream in, TemplateName name) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] buffer = new byte[8192];
    long total = 0;
    int n;
    while ((n = in.read(buffer)) != -1) {
      total += n;
      if (total > maxTemplateBytes) {
        throw new LimitExceededException(
            name.value(), LimitExceededException.Limit.TEMPLATE_SIZE, maxTemplateBytes + " bytes");
      }
      out.write(buffer, 0, n);
    }
    String text = out.toString(StandardCharsets.UTF_8);
    return text.startsWith("﻿") ? text.substring(1) : text;
  }

  @Override
  public List<String> availableTemplates() {
    TreeSet<String> names = new TreeSet<>();
    try {
      Enumeration<URL> urls = classLoader.getResources(root);
      while (urls.hasMoreElements() && names.size() < MAX_LISTED) {
        collect(urls.nextElement(), names);
      }
    } catch (IOException | URISyntaxException | RuntimeException e) {
      // Listing is best effort: it only improves error messages.
    }
    return names.stream().limit(MAX_LISTED).toList();
  }

  private void collect(URL url, TreeSet<String> names) throws IOException, URISyntaxException {
    if ("file".equals(url.getProtocol())) {
      Path base = Path.of(url.toURI());
      try (Stream<Path> files = Files.walk(base, 8)) {
        files
            .filter(Files::isRegularFile)
            .map(p -> base.relativize(p).toString().replace('\\', '/'))
            .forEach(relative -> add(relative, names));
      }
      return;
    }
    URLConnection connection = url.openConnection();
    if (connection instanceof JarURLConnection jar) {
      connection.setUseCaches(false);
      String prefix = jar.getEntryName() == null ? "" : jar.getEntryName();
      if (!prefix.endsWith("/")) {
        prefix = prefix + "/";
      }
      try (JarFile file = jar.getJarFile()) {
        Enumeration<JarEntry> entries = file.entries();
        while (entries.hasMoreElements()) {
          String entry = entries.nextElement().getName();
          if (entry.startsWith(prefix) && !entry.endsWith("/")) {
            add(entry.substring(prefix.length()), names);
          }
        }
      }
    }
  }

  private static void add(String relative, TreeSet<String> names) {
    if (relative.endsWith(".html") && TemplateName.isValid(relative)) {
      names.add(TemplateName.of(relative).value());
    }
  }

  @Override
  public String location() {
    return root;
  }

  /**
   * Returns the normalised class path root.
   *
   * @return for example {@code reports}
   */
  public String root() {
    return root;
  }
}
