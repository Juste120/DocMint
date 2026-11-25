package io.github.juste120.agbale.infrastructure.pdf;

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import io.github.juste120.agbale.domain.FontDefinition;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Fonts embedded in every PDF.
 *
 * <p>Noto Sans (SIL Open Font License 1.1) is always available. It covers French and the Latin
 * letters of Ewe and other West African languages (ɖ, ɛ, ƒ, ɣ, ŋ, ɔ, ʋ). It is registered under
 * {@code Noto Sans} and the generic families {@code sans-serif}, {@code serif} and {@code
 * monospace}, so that text never falls back to the PDF standard fonts, which cannot encode these
 * letters. The output is therefore identical with or without fonts installed on the system.
 *
 * <p>Font files are read once and kept in memory.
 */
public final class FontRegistry {

  /** Family name of the embedded default font. */
  public static final String DEFAULT_FAMILY = "Noto Sans";

  private static final String BASE = "io/github/juste120/agbale/fonts/";
  private static final List<String> DEFAULT_ALIASES =
      List.of(DEFAULT_FAMILY, "sans-serif", "serif", "monospace");
  private static final List<LoadedFont> BUILT_IN = builtIn();

  private final List<LoadedFont> custom;

  /**
   * Creates a registry with the built-in font and additional fonts.
   *
   * @param fonts additional fonts
   * @param classLoader the class loader to read additional fonts from
   * @throws IllegalArgumentException if an additional font cannot be found
   */
  public FontRegistry(List<FontDefinition> fonts, ClassLoader classLoader) {
    List<LoadedFont> loaded = new ArrayList<>();
    for (FontDefinition font : fonts) {
      byte[] bytes = read(classLoader, font.classpathLocation());
      if (bytes == null) {
        throw new IllegalArgumentException(
            "Font file '"
                + font.classpathLocation()
                + "' for family '"
                + font.family()
                + "' was not found on the class path. Put the .ttf file in src/main/resources.");
      }
      loaded.add(new LoadedFont(font.family(), font.weight(), font.italic(), bytes));
    }
    this.custom = List.copyOf(loaded);
  }

  void registerOn(PdfRendererBuilder builder) {
    for (LoadedFont font : BUILT_IN) {
      for (String alias : DEFAULT_ALIASES) {
        font.registerAs(builder, alias);
      }
    }
    for (LoadedFont font : custom) {
      font.registerAs(builder, font.family());
    }
  }

  private static List<LoadedFont> builtIn() {
    ClassLoader loader = FontRegistry.class.getClassLoader();
    return List.of(
        builtIn(loader, "NotoSans-Regular.ttf", FontDefinition.NORMAL, false),
        builtIn(loader, "NotoSans-Bold.ttf", FontDefinition.BOLD, false),
        builtIn(loader, "NotoSans-Italic.ttf", FontDefinition.NORMAL, true),
        builtIn(loader, "NotoSans-BoldItalic.ttf", FontDefinition.BOLD, true));
  }

  private static LoadedFont builtIn(ClassLoader loader, String file, int weight, boolean italic) {
    byte[] bytes = read(loader, BASE + file);
    if (bytes == null) {
      throw new IllegalStateException("Agbale embedded font is missing: " + file);
    }
    return new LoadedFont(DEFAULT_FAMILY, weight, italic, bytes);
  }

  private static byte[] read(ClassLoader loader, String location) {
    try (InputStream in = loader.getResourceAsStream(location)) {
      return in == null ? null : in.readAllBytes();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private record LoadedFont(String family, int weight, boolean italic, byte[] bytes) {
    void registerAs(PdfRendererBuilder builder, String alias) {
      builder.useFont(
          () -> new ByteArrayInputStream(bytes),
          alias,
          weight,
          italic ? FontStyle.ITALIC : FontStyle.NORMAL,
          true);
    }
  }
}
