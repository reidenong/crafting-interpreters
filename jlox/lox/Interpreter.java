package jlox.lox;

/**
 * Interprets (the nodes in) an AST.
 * Acts as a concrete implementation for the Visitor interface.
 * 
 * Expr has an accept(Visitor) that uses the Visitor to evaluate it.
 * Here, Interpreter is a Visitor object that on accept() returns
 * the value of the Expr.
 */
class Interpreter implements Expr.Visitor<Object> {

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
        return -(double) right;
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
        return (double)left - (double)right;
      case SLASH:
        return (double)left / (double)right;
      case STAR:
        return (double)left * (double)right;
      case PLUS:
        if (left isinstance double && right isinstance double) {
          return (double)left + (double)right;
        }
        if (left isinstance String && right isinstance String) {
          return (String)left + (String)right;
        }
      case GREATER:
        return (double)left > (double)right;
      case GREATER_EQUAL:
        return (double)left >= (double)right;
      case LESS:
        return (double)left < (double)right;
      case LESS_EQUAL:
        return (double)left <= (double)right;
      case BANG_EQUAL: return !isEqual(left, right);
      case EQUAL_EQUAL: return isEqual(left, right);
      break;
    }

    // Unreachable
    return null;
  }

  // Helper Methods

  // Evaluate subexpression
  private Object evaluate(Expr expr) {
    return expr.accept(this);
  }

  // Evaluate Truthy-ness of an Object
  // false and nil(null) are falsey, everything else is truthy
  private boolean isTruthy(Object object) {
    if (object == null)
      return false;
    if (object instanceof boolean)
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