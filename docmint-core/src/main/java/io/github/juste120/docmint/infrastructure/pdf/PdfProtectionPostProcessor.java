package io.github.juste120.docmint.infrastructure.pdf;

import io.github.juste120.docmint.api.RenderException;
import io.github.juste120.docmint.domain.OutputFormat;
import io.github.juste120.docmint.domain.Permission;
import io.github.juste120.docmint.domain.Protection;
import io.github.juste120.docmint.domain.RenderedDocument;
import io.github.juste120.docmint.port.PostProcessContext;
import io.github.juste120.docmint.port.PostProcessor;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Set;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;

/**
 * Encrypts a PDF with AES 256-bit and applies permissions.
 *
 * <p>When no owner password is given, a random one is generated (and forgotten), so the permissions
 * cannot be lifted. Passwords are never logged nor put in error messages.
 */
public final class PdfProtectionPostProcessor implements PostProcessor {

  private static final int KEY_LENGTH = 256;
  private static final SecureRandom RANDOM = new SecureRandom();

  @Override
  public boolean appliesTo(RenderedDocument document, PostProcessContext context) {
    return PostProcessor.is(document, OutputFormat.PDF) && context.options().protection() != null;
  }

  @Override
  public RenderedDocument process(RenderedDocument document, PostProcessContext context) {
    Protection protection = context.options().protection();
    char[] open = protection.openPasswordCopy();
    char[] owner = protection.ownerPasswordCopy();
    try (PDDocument pdf = Loader.loadPDF(document.content())) {
      String ownerPassword = owner != null ? new String(owner) : randomPassword();
      String openPassword = open != null ? new String(open) : "";
      StandardProtectionPolicy policy =
          new StandardProtectionPolicy(
              ownerPassword, openPassword, permissions(protection.grantedPermissions()));
      policy.setEncryptionKeyLength(KEY_LENGTH);
      pdf.protect(policy);
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      pdf.save(out);
      return new RenderedDocument(out.toByteArray(), OutputFormat.PDF, document.pageCount());
    } catch (IOException e) {
      // The cause may mention internal details, never the password: keep only its type.
      throw new RenderException(
          "The PDF could not be encrypted (" + e.getClass().getSimpleName() + ").", null);
    } finally {
      if (open != null) {
        Arrays.fill(open, '\0');
      }
      if (owner != null) {
        Arrays.fill(owner, '\0');
      }
    }
  }

  private static AccessPermission permissions(Set<Permission> granted) {
    AccessPermission permission = new AccessPermission();
    permission.setCanPrint(granted.contains(Permission.PRINT));
    permission.setCanPrintFaithful(granted.contains(Permission.PRINT_HIGH_QUALITY));
    permission.setCanExtractContent(granted.contains(Permission.COPY));
    permission.setCanModify(granted.contains(Permission.MODIFY));
    permission.setCanModifyAnnotations(granted.contains(Permission.ANNOTATE));
    permission.setCanFillInForm(granted.contains(Permission.FILL_FORMS));
    permission.setCanExtractForAccessibility(
        granted.contains(Permission.EXTRACT_FOR_ACCESSIBILITY));
    permission.setCanAssembleDocument(granted.contains(Permission.ASSEMBLE));
    return permission;
  }

  private static String randomPassword() {
    byte[] bytes = new byte[32];
    RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}
