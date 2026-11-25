package io.github.juste120.docmint.domain;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Password protection of a PDF (AES 256-bit encryption).
 *
 * <ul>
 *   <li>The <em>open password</em>, if set, is required to open the document.
 *   <li>The <em>permissions</em> say what readers may do without the <em>owner password</em>. When
 *       no owner password is given, a random one is generated, so restrictions cannot be removed.
 * </ul>
 *
 * <p>Passwords are kept as {@code char[]} copies, never appear in {@link #toString()}, and can be
 * wiped with {@link #clear()}. Instances are immutable from the outside.
 */
public final class Protection {

  private final char[] openPassword;
  private final char[] ownerPassword;
  private final Set<Permission> permissions;

  private Protection(char[] openPassword, char[] ownerPassword, Set<Permission> permissions) {
    this.openPassword = copy(openPassword);
    this.ownerPassword = copy(ownerPassword);
    this.permissions =
        permissions.isEmpty()
            ? Set.of()
            : Set.copyOf(EnumSet.copyOf(Objects.requireNonNull(permissions, "permissions")));
  }

  /**
   * Requires a password to open the document. Every permission is granted once it is open; use
   * {@link #withPermissions(Permission...)} to restrict them.
   *
   * @param password the password, not empty
   * @return the protection
   * @throws IllegalArgumentException if the password is empty
   */
  public static Protection openPassword(char[] password) {
    if (password == null || password.length == 0) {
      throw new IllegalArgumentException("the open password is empty");
    }
    return new Protection(password, null, EnumSet.allOf(Permission.class));
  }

  /**
   * No open password, but only the given permissions (e.g. print only, no copy).
   *
   * @param allowed the allowed operations (may be empty: read only)
   * @return the protection
   */
  public static Protection permissions(Permission... allowed) {
    return new Protection(null, null, toSet(allowed));
  }

  /**
   * Returns a copy restricted to the given permissions.
   *
   * @param allowed the allowed operations
   * @return the new protection
   */
  public Protection withPermissions(Permission... allowed) {
    return new Protection(openPassword, ownerPassword, toSet(allowed));
  }

  /**
   * Returns a copy with an explicit owner password (otherwise a random one is generated).
   *
   * @param password the owner password, not empty
   * @return the new protection
   */
  public Protection withOwnerPassword(char[] password) {
    if (password == null || password.length == 0) {
      throw new IllegalArgumentException("the owner password is empty");
    }
    return new Protection(openPassword, password, permissions);
  }

  /**
   * Returns a copy of the open password.
   *
   * @return the password, or {@code null} if the document opens without password
   */
  public char[] openPasswordCopy() {
    return copy(openPassword);
  }

  /**
   * Returns a copy of the owner password.
   *
   * @return the password, or {@code null} if a random one must be generated
   */
  public char[] ownerPasswordCopy() {
    return copy(ownerPassword);
  }

  /**
   * Returns the granted permissions.
   *
   * @return an immutable set
   */
  public Set<Permission> grantedPermissions() {
    return permissions;
  }

  /** Overwrites the passwords held by this object with zeros. */
  public void clear() {
    if (openPassword != null) {
      Arrays.fill(openPassword, '\0');
    }
    if (ownerPassword != null) {
      Arrays.fill(ownerPassword, '\0');
    }
  }

  private static Set<Permission> toSet(Permission... allowed) {
    Set<Permission> set = EnumSet.noneOf(Permission.class);
    for (Permission permission : Objects.requireNonNull(allowed, "permissions")) {
      set.add(Objects.requireNonNull(permission, "permission"));
    }
    return set;
  }

  private static char[] copy(char[] value) {
    return value == null ? null : value.clone();
  }

  @Override
  public String toString() {
    return "Protection[openPassword="
        + (openPassword == null ? "none" : "***")
        + ", permissions="
        + permissions
        + "]";
  }
}
