package jlox.lox;

import java.util.ArrayList;
import java.util.List;

/**
 * Interprets (the nodes in) an AST.
 * Acts as a concrete implementation for the Visitor interface.
 * 
 * Expr has an accept(Visitor) that uses the Visitor to evaluate it.
 * Here, Interpreter is a Visitor object that on accept() returns
 * the value of the Expr.
 */
class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
  final Environment globals = new Environment();
  private Environment environment = globals;

  Interpreter() {
    // Stuff native clock function into globals.
    // Java Anonymous class that implements LoxCallable.
    globals.define("clock", new LoxCallable() {
      @Override
      public int arity() {
        return 0;
      }

      @Override
      public Object call(Interpreter interpreter, List<Object> arguments) {
        return (double) System.currentTimeMillis() / 1000.0;
      }

      @Override
      public String toString() {
        return "<native fn>";
      }
    });
  }

  void interpret(List<Stmt> statements) {
    try {
      for (Stmt statement : statements) {
        execute(statement);
      }
    } catch (RuntimeError error) {
      Lox.runtimeError(error);
    }
  }

  private String stringify(Object object) {
    if (object == null)
      return "nil";

    if (object instanceof Double) {
      String text = object.toString();
      if (text.endsWith(".0")) {
        text = text.substring(0, text.length() - 2);
      }
      return text;
    }

    return object.toString();
  }

  // ======================
  // Evaluating Statements
  // ======================

  @Override
  public Void visitIfStmt(Stmt.If stmt) {
    if (isTruthy(evaluate(stmt.condition))) {
      execute(stmt.thenBranch);
    } else if (stmt.elseBranch != null) {
      execute(stmt.elseBranch);
    }
    return null;
  }

  @Override
  public Void visitBlockStmt(Stmt.Block stmt) {
    executeBlock(stmt.statements, new Environment(environment)); // Create new environment.
    return null;
  }

  @Override
  public Void visitVarStmt(Stmt.Var stmt) {
    Object value = null;
    if (stmt.initializer != null)
      value = evaluate(stmt.initializer);

    environment.define(stmt.name.lexeme, value);
    return null;
  }

  @Override
  public Void visitWhileStmt(Stmt.While stmt) {
    while (isTruthy(evaluate(stmt.condition))) {
      execute(stmt.body);
    }
    return null;
  }

  /**
   * Interpret a function declaration.
   */
  @Override
  public Void visitFunctionStmt(Stmt.Function stmt) {
    LoxFunction function = new LoxFunction(stmt, environment);
    environment.define(stmt.name.lexeme, function);
    return null;
  }

  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    evaluate(stmt.expression);
    return null;
  }

  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    Object value = evaluate(stmt.expression);
    System.out.println(stringify(value));
    return null;
  }

  @Override
  public Void visitReturnStmt(Stmt.Return stmt) {
    Object value = null;
    if (stmt.value != null)
      value = evaluate(stmt.value);

    throw new Return(value);
  }

  // ======================
  // Evaluating Expressions
  // ======================

  // Evaluating a assignment expression
  @Override
  public Object visitAssignExpr(Expr.Assign expr) {
    Object value = evaluate(expr.value);
    environment.assign(expr.name, value);
    return value;
  }

  // Evaluating a variable expression
  @Override
  public Object visitVariableExpr(Expr.Variable expr) {
    return environment.get(expr.name);
  }

  // Evaluating a Literal.
  // Simply return the value
  @Override
  public Object visitLiteralExpr(Expr.Literal expr) {
    return expr.value;
  }

  // Evaluating a OR expression
  @Override
  public Object visitLogicalExpr(Expr.Logical expr) {
    Object left = evaluate(expr.left);

    if (expr.operator.type == TokenType.OR) {
      // Short circuit if left is truthy
      if (isTruthy(left))
        return left;
    } else {
      // Short circuit if left is not truthy
      if (!isTruthy(left))
        return left;
    }

    return evaluate(expr.right);
  }

  // Evaluating a grouping "()"
  // Send the expression back into the visitor implementation
  @Override
  public Object visitGroupingExpr(Expr.Grouping expr) {
    return evaluate(expr.expression);
  }

  // Evaluating a Unary Expr
  @Override
  public Object visitUnaryExpr(Expr.Unary expr) {
    Object right = evaluate(expr.right);

    switch (expr.operator.type) {
      case BANG:
        return !isTruthy(right);
      case MINUS:
        checkNumberOperand(expr.operator, right);
        return -(double) right;
      default:
        break;
    }

    // Unreachable
    return null;
  }

  // Evaluating a Call expression
  @Override
  public Object visitCallExpr(Expr.Call expr) {
    Object callee = evaluate(expr.callee);

    List<Object> arguments = new ArrayList<>();
    for (Expr argument : expr.arguments) {
      arguments.add(evaluate(argument));
    }

    // Cast function to LoxCallable and call
    if (!(callee instanceof LoxCallable)) {
      throw new RuntimeError(expr.paren,
          "Can only call functions and classes.");
    }

    LoxCallable function = (LoxCallable) callee;

    // Check function arity is strictly equal.
    if (arguments.size() != function.arity()) {
      throw new RuntimeError(expr.paren,
          String.format("Expected %s arguments but got %s.", function.arity(), arguments.size()));
    }
    return function.call(this, arguments);
  }

  // Evaluating a Binary Expr
  @Override
  public Object visitBinaryExpr(Expr.Binary expr) {
    Object left = evaluate(expr.left);
    Object right = evaluate(expr.right);

    switch (expr.operator.type) {
      case MINUS:
        // Dynamic typing -> Typecasts at runtime
        checkNumberOperand(expr.operator, left, right);
        return (double) left - (double) right;
      case SLASH:
        checkNumberOperand(expr.operator, left, right);
        return (double) left / (double) right;
      case STAR:
        checkNumberOperand(expr.operator, left, right);
        return (double) left * (double) right;
      case PLUS:
        if (left instanceof Double && right instanceof Double) {
          return (double) left + (double) right;
        }
        if (left instanceof String && right instanceof String) {
          return (String) left + (String) right;
        }

        throw new RuntimeError(expr.operator, "Operands must be two numbers or strings.");
      case GREATER:
        checkNumberOperand(expr.operator, left, right);
        return (double) left > (double) right;
      case GREATER_EQUAL:
        checkNumberOperand(expr.operator, left, right);
        return (double) left >= (double) right;
      case LESS:
        checkNumberOperand(expr.operator, left, right);
        return (double) left < (double) right;
      case LESS_EQUAL:
        checkNumberOperand(expr.operator, left, right);
        return (double) left <= (double) right;
      case BANG_EQUAL:
        return !isEqual(left, right);
      case EQUAL_EQUAL:
        return isEqual(left, right);
      default:
        break;
    }

    // Unreachable
    return null;
  }

  // ==============
  // Error handling
  // ==============

  // Check if operand is a number
  private void checkNumberOperand(Token operator, Object operand) {
    if (operand instanceof Double)
      return;
    throw new RuntimeError(operator, "Operand must be a number");
  }

  private void checkNumberOperand(Token operator, Object left, Object right) {
    if (left instanceof Double && right instanceof Double)
      return;
    throw new RuntimeError(operator, "Operand must be a number");
  }

  // ==============
  // Helper Methods
  // ==============

  // Evaluate subexpression
  private Object evaluate(Expr expr) {
    return expr.accept(this);
  }

  // Execute statements in a block.
  Void executeBlock(List<Stmt> statements, Environment environment) {
    Environment prev = this.environment; // Remember previous environment.
    try {
      this.environment = environment; // Use new scope.
      for (Stmt stmt : statements) {
        execute(stmt);
      }
    } finally {
      this.environment = prev; // Change back to previous scope.
    }
    return null;
  }

  // Execute statement
  private void execute(Stmt stmt) {
    stmt.accept(this);
  }

  // Evaluate Truthy-ness of an Object
  // false and nil(null) are falsey, everything else is truthy
  private boolean isTruthy(Object object) {
    if (object == null)
      return false;
    if (object instanceof Boolean)
      return (boolean) object;
    return true;
  }

  // Evaluate equality of two objects
  private boolean isEqual(Object a, Object b) {
    if (a == null && b == null)
      return true;
    if (a == null)
      return false;
    return a.equals(b);
  }
}