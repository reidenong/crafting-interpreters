package jlox.lox;

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

  // ======================
  // Evaluating Expressions
  // ======================

  // Evaluating a Literal.
  // Simply return the value
  @Override
  public Object visitLiteralExpr(Expr.Literal expr) {
    return expr.value;
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