package io.github.juste120.docmint.domain;

import java.net.URI;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Decides which remote hosts a document may load resources (images, stylesheets, fonts) from.
 *
 * <p>By default the set is empty: documents only use class path resources and {@code data:} URIs,
 * so rendering never opens a network connection (protection against SSRF, server-side request
 * forgery). A host must be allow-listed explicitly, and only {@code https} is ever allowed for
 * remote resources. An entry is either an exact host ({@code cdn.example.com}) or a sub-domain
 * wildcard ({@code *.example.com}, which does not match {@code example.com} itself).
 *
 * @param allowedHosts the allow-listed hosts, lower-case
 */
public record ResourceAccessPolicy(Set<String> allowedHosts) {

  /**
   * Creates a policy, validating and normalising the hosts.
   *
   * @throws IllegalArgumentException if a host entry is invalid
   */
  public ResourceAccessPolicy {
    Set<String> normalised = new TreeSet<>();
    for (String host : allowedHosts) {
      normalised.add(normalise(host));
    }
    allowedHosts = Set.copyOf(normalised);
  }

  /**
   * Returns the default policy, which allows no remote host.
   *
   * @return a policy with an empty allow-list
   */
  public static ResourceAccessPolicy denyAllRemote() {
    return new ResourceAccessPolicy(Set.of());
  }

  /**
   * Returns a copy of this policy that also allows the given host.
   *
   * @param host an exact host or a {@code *.domain} wildcard
   * @return the new policy
   */
  public ResourceAccessPolicy allow(String host) {
    Set<String> hosts = new TreeSet<>(allowedHosts);
    hosts.add(normalise(host));
    return new ResourceAccessPolicy(hosts);
  }

  /**
   * Returns {@code true} if the given absolute remote URI may be loaded.
   *
   * @param uri an absolute URI
   * @return {@code true} only for {@code https} URIs whose host is allow-listed
   */
  public boolean allowsRemote(String uri) {
    if (uri == null || allowedHosts.isEmpty()) {
      return false;
    }
    URI parsed;
    try {
      parsed = URI.create(uri);
    } catch (IllegalArgumentException e) {
      return false;
    }
    if (!"https".equalsIgnoreCase(parsed.getScheme())
        || parsed.getHost() == null
        || parsed.getRawUserInfo() != null) {
      return false;
    }
    String host = parsed.getHost().toLowerCase(Locale.ROOT);
    for (String allowed : allowedHosts) {
      if (allowed.startsWith("*.")) {
        if (host.endsWith(allowed.substring(1))) {
          return true;
        }
      } else if (host.equals(allowed)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns {@code true} if an entry uses a wildcard, which deserves a warning.
   *
   * @return whether a {@code *.} entry exists
   */
  public boolean hasWildcard() {
    return allowedHosts.stream().anyMatch(h -> h.startsWith("*."));
  }

  private static String normalise(String host) {
    if (host == null || host.isBlank()) {
      throw new IllegalArgumentException("an allowed host is empty");
    }
    String value = host.strip().toLowerCase(Locale.ROOT);
    String bare = value.startsWith("*.") ? value.substring(2) : value;
    if (bare.isEmpty()
        || bare.contains("*")
        || bare.contains("/")
        || bare.contains(":")
        || bare.contains("@")
        || bare.startsWith(".")
        || bare.endsWith(".")) {
      throw new IllegalArgumentException(
          "invalid allowed host '"
              + host
              + "': use a host name such as 'cdn.example.com' or"
              + " '*.example.com', without scheme, port or path");
    }
    return value;
  }
}
