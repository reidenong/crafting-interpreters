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
  private final Environment closure;

  private final boolean isInitializer;

  LoxFunction(Stmt.Function declaration, Environment closure, boolean isInitializer) {
    this.declaration = declaration;
    this.closure = closure;
    this.isInitializer = isInitializer;
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
    Environment environment = new Environment(closure);
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
      if (isInitializer)
        return closure.getAt(0, "this"); // Return this if early return in an init.
      return returnValue.value;
    }

    if (isInitializer)
      return closure.getAt(0, "this");
    return null;
  }

  // Bind the LoxInstance to the method, when method is called, it becomes the
  // parent of the method body's environment.
  LoxFunction bind(LoxInstance instance) {
    Environment environment = new Environment(closure);
    environment.define("this", instance);
    return new LoxFunction(declaration, environment, isInitializer);
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
