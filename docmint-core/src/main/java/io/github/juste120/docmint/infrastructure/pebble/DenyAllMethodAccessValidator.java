package io.github.juste120.docmint.infrastructure.pebble;

import io.pebbletemplates.pebble.attributes.methodaccess.MethodAccessValidator;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Refuses every method call from templates, except the getters of Pebble's own loop variables.
 *
 * <p>Templates only receive maps, lists and simple values (see the template model mapper), and read
 * them through Pebble's map and list resolvers, which never invoke methods. Denying method access
 * is a second, independent barrier: {@code {{ x.getClass() }}} or any other call fails.
 *
 * <p>The only exception is the {@code loop} variable of {@code for} tags ({@code loop.index},
 * {@code loop.first}, {@code loop.last}, {@code loop.length}, {@code loop.revindex}): Pebble
 * implements it as an object, and only its no-argument public getters are allowed.
 */
final class DenyAllMethodAccessValidator implements MethodAccessValidator {

  private static final String LOOP_VARIABLES =
      "io.pebbletemplates.pebble.node.ForNode$LoopVariables";
  private static final String LOOP_VALUES = "io.pebbletemplates.pebble.node.fornode.";

  @Override
  public boolean isMethodAccessAllowed(Object object, Method method) {
    if (object == null
        || method.getParameterCount() != 0
        || Modifier.isStatic(method.getModifiers())
        || method.getDeclaringClass() == Object.class) {
      return false;
    }
    String type = object.getClass().getName();
    String name = method.getName();
    boolean getter = name.startsWith("get") || name.startsWith("is");
    return getter && (type.equals(LOOP_VARIABLES) || type.startsWith(LOOP_VALUES));
  }
}
