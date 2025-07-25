package jlox.lox;

import java.util.List;

/**
 * Implements a Lox Function Object.
 * 
 * Note:
 * Parameters are the variables in the function definition, they have names.
 * Arguments are the values passed to a function, they have values.
 */
class LoxFunction implements LoxCallable {
  private final Stmt.Function declaration;

  LoxFunction(Stmt.Function declaration) {
    this.declaration = declaration;
  }

  /**
   * Implements a function call.
   * 
   * We create a new environment (scope) which contains the arguments from the
   * function call, and then we run the block.
   */
  @Override
  public Object call(Interpreter interpreter, List<Object> arguments) {
    // Create the new environment with arguments.
    Environment environment = new Environment(interpreter.globals);
    for (int i = 0; i < declaration.params.size(); i++) {
      environment.define(declaration.params.get(i).lexeme, arguments.get(i));
    }

    /*
     * Execute the function in its own scope (Environment).
     * 
     * Use exceptions as return control-flow:
     * When we catch a return exception, pull out of the value and make the return
     * value from call().
     * 
     * if there is no return statement, implicitly return null.
     */
    try {
      interpreter.executeBlock(declaration.body, environment);
    } catch (Return returnValue) {
      return returnValue.value;
    }
    return null;
  }

  @Override
  public int arity() {
    return declaration.params.size();
  }

  @Override
  public String toString() {
    return String.format("<fn %s>", declaration.name.lexeme);
  }
}
