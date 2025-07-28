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
    LoxFunction initializer = findMethod("init");

    // If we find a initializer, we bind and invoke it like a regular method call.
    if (initializer != null) {
      initializer.bind(instance).call(interpreter, arguments);
    }
    return instance;
  }

  @Override
  public int arity() {
    LoxFunction initializer = findMethod("init");
    if (initializer == null)
      return 0;
    return arity();
  }

  @Override
  public String toString() {
    return String.format("<class %s>", name);
  }
}
