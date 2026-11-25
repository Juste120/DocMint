package io.github.juste120.agbale.infrastructure.pebble;

import io.pebbletemplates.pebble.attributes.methodaccess.MethodAccessValidator;
import java.lang.reflect.Method;

/**
 * Refuses every method call from templates.
 *
 * <p>Templates only receive maps, lists and simple values (see the template model mapper), and read
 * them through Pebble's map and list resolvers, which never invoke methods. Denying all method
 * access is a second, independent barrier: {@code {{ x.getClass() }}} or any other call fails.
 */
final class DenyAllMethodAccessValidator implements MethodAccessValidator {

  @Override
  public boolean isMethodAccessAllowed(Object object, Method method) {
    return false;
  }
}
