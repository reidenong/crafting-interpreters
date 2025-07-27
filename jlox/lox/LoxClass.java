package jlox.lox;

import java.util.List;
import java.util.Map;

class LoxClass implements LoxCallable {
  final String name;

  LoxClass(String name) {
    this.name = name;
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
