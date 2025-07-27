package jlox.lox;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores the bindings that associate variables to values.
 */
class Environment {
  final Environment enclosing; // Reference to parent environment.
  private final Map<String, Object> values = new HashMap<>();

  // Constructors for making a new environment.

  // No envlosing environment for global scope.
  Environment() {
    enclosing = null;
  }

  Environment(Environment enclosing) {
    this.enclosing = enclosing;
  }

  // Methods to mutate environment.

  void assign(Token name, Object value) {
    if (values.containsKey(name.lexeme)) {
      values.put(name.lexeme, value);
      return;
    }

    if (enclosing != null) {
      enclosing.assign(name, value);
      return;
    }

    throw new RuntimeError(name, String.format("Undefined variable '%s'.", name));
  }

  void define(String name, Object value) {
    values.put(name, value);
  }

  Environment ancestor(int distance) {
    Environment environment = this;
    for (int i = 0; i < distance; i++) {
      environment = environment.enclosing;
    }
    return environment;
  }

  Object getAt(int distance, String name) {
    return ancestor(distance).values.get(name);
  }

  void assignAt(int distance, Token name, Object value) {
    ancestor(distance).values.put(name.lexeme, value);
  }

  Object get(Token name) {
    if (values.containsKey(name.lexeme))
      return values.get(name.lexeme);

    // Check enclosing scope.
    if (enclosing != null)
      return enclosing.get(name);

    throw new RuntimeError(name, String.format("Undefined variable %s.", name.lexeme));
  }
}
