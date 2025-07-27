package jlox.lox;

import java.util.HashMap;
import java.util.Map;

class LoxInstance {
  private LoxClass klass;
  private final Map<String, Object> fields = new HashMap<>();

  LoxInstance(LoxClass klass) {
    this.klass = klass;
  }

  /*
   * Looking up a property on an instance.
   */
  Object get(Token name) {
    if (fields.containsKey(name.lexeme)) {
      return fields.get(name.lexeme);
    }

    throw new RuntimeError(name, String.format("Undefined property %s.", name.lexeme));
  }

  @Override
  public String toString() {
    return String.format("<%s instance>", klass.name);
  }
}
