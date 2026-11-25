package io.github.juste120.docmint.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.juste120.docmint.api.DocMint;
import io.github.juste120.docmint.api.ResourceRefusedException;
import io.github.juste120.docmint.domain.MissingResourceMode;
import io.github.juste120.docmint.domain.ResourceAccessPolicy;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** FR-011, FR-012: no remote or file system resource unless an https host is allow-listed. */
class RemoteResourceTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        "http://169.254.169.254/latest/meta-data/",
        "https://evil.example.com/logo.png",
        "file:///etc/hostname",
        "jar:file:/tmp/x.jar!/logo.png",
        "ftp://example.com/logo.png",
        "//evil.example.com/logo.png"
      })
  void refusesRemoteAndFileResources(String url) {
    DocMint docMint = DocMint.createDefault();

    assertThatThrownBy(() -> docMint.pdf("security/remote-image", Map.of("url", url)))
        .isInstanceOf(ResourceRefusedException.class)
        .hasMessageContaining("not allowed")
        .hasMessageNotContaining("/etc/hostname");
  }

  @Test
  void refusedResourcesFailEvenInWarnMode() {
    DocMint docMint = DocMint.builder().missingResources(MissingResourceMode.WARN).build();

    assertThatThrownBy(
            () ->
                docMint.pdf(
                    "security/remote-image", Map.of("url", "https://evil.example.com/a.png")))
        .isInstanceOf(ResourceRefusedException.class);
  }

  @Test
  void refusesRemoteStylesheetsAndImports() {
    DocMint docMint = DocMint.createDefault();

    assertThatThrownBy(() -> docMint.pdf("security/remote-css", Map.of()))
        .isInstanceOf(ResourceRefusedException.class);
    assertThatThrownBy(() -> docMint.pdf("security/css-import", Map.of()))
        .isInstanceOf(ResourceRefusedException.class);
  }

  @Test
  void allowListedHostIsStillRefusedForOtherHostsAndHttp() {
    DocMint docMint = DocMint.builder().allowRemoteHost("cdn.example.com").build();

    assertThatThrownBy(
            () ->
                docMint.pdf(
                    "security/remote-image", Map.of("url", "https://evil.example.com/a.png")))
        .isInstanceOf(ResourceRefusedException.class);
    assertThatThrownBy(
            () ->
                docMint.pdf("security/remote-image", Map.of("url", "http://cdn.example.com/a.png")))
        .isInstanceOf(ResourceRefusedException.class);
  }

  @Test
  void policyAllowsOnlyHttpsOnAllowListedHosts() {
    ResourceAccessPolicy policy =
        ResourceAccessPolicy.denyAllRemote().allow("cdn.example.com").allow("*.images.example.org");

    assertThat(policy.allowsRemote("https://cdn.example.com/logo.png")).isTrue();
    assertThat(policy.allowsRemote("https://a.images.example.org/logo.png")).isTrue();
    assertThat(policy.allowsRemote("https://images.example.org/logo.png")).isFalse();
    assertThat(policy.allowsRemote("http://cdn.example.com/logo.png")).isFalse();
    assertThat(policy.allowsRemote("https://cdn.example.com.evil.net/logo.png")).isFalse();
    assertThat(policy.allowsRemote("https://user@cdn.example.com/logo.png")).isFalse();
    assertThat(ResourceAccessPolicy.denyAllRemote().allowsRemote("https://cdn.example.com/a"))
        .isFalse();
  }
}
