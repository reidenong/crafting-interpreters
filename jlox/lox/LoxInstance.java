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

    LoxFunction method = klass.findMethod(name.lexeme);
    if (method != null)
      // Return the method with a bound LoxInstance which it is being called from.
      return method.bind(this);

    throw new RuntimeError(name, String.format("Undefined property %s.", name.lexeme));
  }

  /*
   * Setting a property on an instance
   * 
   * - Lox allows freely creating new fields on instances, no need to check if the
   * key is actually present.
   */
  void set(Token name, Object value) {
    fields.put(name.lexeme, value);
  }

  @Override
  public String toString() {
    return String.format("<%s instance>", klass.name);
  }
}
