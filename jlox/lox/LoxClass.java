package jlox.lox;

import java.util.List;
import java.util.Map;

/*
 * Describes a class in Lox.
 * 
 * - Implements LoxCallable for the constructor.
 * - Instances store state, classes stores behaviour.
 * - LoxInstance stores a map of fields, LoxClass stores a map of methods.
 */
class LoxClass implements LoxCallable {
  final String name;
  private final Map<String, LoxFunction> methods;

  LoxClass(String name, Map<String, LoxFunction> methods) {
    this.name = name;
    this.methods = methods;
  }

  LoxFunction findMethod(String name) {
    if (methods.containsKey(name))
      return methods.get(name);
    return null;
  }

  @Override
  public Object call(Interpreter interpreter, List<Object> arguments) {
    LoxInstance instance = new LoxInstance(this);
    return instance;
  }

  @Override
  public int arity() {
    return 0;
  }

  @Override
  public String toString() {
    return String.format("<class %s>", name);
  }
}
