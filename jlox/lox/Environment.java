package jlox.lox;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores the bindings that associate variables to values.
 */
class Environment {
  // Global variables
  private final Map<String, Object> values = new HashMap<>();

  void assign(Token name, Object value) {
    if (values.containsKey(name.lexeme)) {
      values.put(name.lexeme, value);
      return;
    }
    throw new RuntimeError(name, String.format("Undefined variable '%s'.", name));
  }

  void define(String name, Object value) {
    values.put(name, value);
  }

  Object get(Token name) {
    if (values.containsKey(name.lexeme))
      return values.get(name.lexeme);
    throw new RuntimeError(name, String.format("Undefined variable %s.", name.lexeme));
  }
}
