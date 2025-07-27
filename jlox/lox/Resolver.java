package jlox.lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Resolver walks the AST to determine the appropriate scope for each variable
 * usage.
 * 
 * We care about visiting the following nodes:
 * - Block statements that introduce new scope for its statements
 * - Function declarations that introduce new scope for its body and binds
 * parameters
 * - Variable declarations add new variable to the current scope
 * - Variable and assignment expressions need to have variables resolved
 */
class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
  private final Interpreter interpreter;
  private final Stack<Map<String, Boolean>> scopes = new Stack<>();
  private FunctionType currentFunction = FunctionType.NONE;

  private enum FunctionType {
    NONE,
    FUNCTION
  }

  /*
   * We poke all the resolution data directly into it as we walk over variables.
   * When the interpreter runs after, it has everything it needs.
   */
  Resolver(Interpreter interpreter) {
    this.interpreter = interpreter;
  }

  /**
   * Visits a block statement node.
   * 
   * We begin a new scope, traverse into those statements, then resolve the scope.
   */
  @Override
  public Void visitBlockStmt(Stmt.Block stmt) {
    beginScope();
    resolve(stmt.statements);
    endScope();
    return null;
  }

  /*
   * We set a variable declaration to be a 3 step process,
   * 1. We declare the variable to exist but not ready
   * 2. We resolve the statement initializer
   * 3. We declare the variable to be ready
   */
  @Override
  public Void visitVarStmt(Stmt.Var stmt) {
    declare(stmt.name);
    if (stmt.initializer != null) {
      resolve(stmt.initializer);
    }
    define(stmt.name);
    return null;
  }

  @Override
  public Void visitVariableExpr(Expr.Variable expr) {
    // Check that variable has been declared AND defined.
    if (!scopes.isEmpty() && scopes.peek().get(expr.name.lexeme) == Boolean.FALSE) {
      Lox.error(expr.name, "Can't read local variable in its own initializer.");
    }
    resolveLocal(expr, expr.name);
    return null;
  }

  @Override
  public Void visitAssignExpr(Expr.Assign expr) {
    resolve(expr.value);
    resolveLocal(expr, expr.name);
    return null;
  }

  /*
   * Resolving function declarations.
   * 
   * Functions both bind names and introduce a scope.
   * - Name of the function is bound in the surrounding scope where the function
   * is declared.
   * - When we step into the function body, we also bind its parameteres to the
   * inner function scope.
   */
  @Override
  public Void visitFunctionStmt(Stmt.Function stmt) {
    declare(stmt.name);
    define(stmt.name);
    resolveFunction(stmt, FunctionType.FUNCTION);
    return null;
  }

  // =================
  // HELPER FUNCTIONS
  // =================

  private void resolveFunction(Stmt.Function function, FunctionType type) {
    FunctionType enclosingFunction = currentFunction;
    currentFunction = type;

    beginScope();
    for (Token param : function.params) { // Bind variables for each function parameter.
      declare(param);
      define(param);
    }
    resolve(function.body);
    endScope();

    currentFunction = enclosingFunction;
  }

  /*
   * When evaluating a variable, start at innermost scope and work outwards, and
   * resolve a variable if we find it.
   * 
   * We resolve a variable by adding the environment depth at which to collect the
   * node into the interpreter, so that during it's own run it is aware of which
   * lexical scope the variable belongs to.
   */
  private void resolveLocal(Expr expr, Token name) {
    for (int i = scopes.size() - 1; i >= 0; i--) {
      if (scopes.get(i).containsKey(name.lexeme)) {
        interpreter.resolve(expr, scopes.size() - 1 - i);
        return;
      }
    }
  }

  // Adding a variable to the current scope
  private void declare(Token name) {
    if (scopes.isEmpty())
      return;

    Map<String, Boolean> scope = scopes.peek();
    if (scope.containsKey(name.lexeme)) {
      Lox.error(name, "Already a variable with this name in this scope.");
    }
    scope.put(name.lexeme, false);
  }

  private void define(Token name) {
    if (scopes.isEmpty())
      return;
    scopes.peek().put(name.lexeme, true);
  }

  private void beginScope() {
    scopes.push(new HashMap<String, Boolean>());
  }

  private void endScope() {
    scopes.pop();
  }

  void resolve(List<Stmt> statements) {
    for (Stmt statement : statements)
      resolve(statement);
  }

  private void resolve(Stmt stmt) {
    stmt.accept(this);
  }

  private void resolve(Expr expr) {
    expr.accept(this);
  }

  // =================================
  // Resolving other syntax tree nodes
  // =================================

  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    resolve(stmt.expression);
    return null;
  }

  @Override
  public Void visitIfStmt(Stmt.If stmt) {
    resolve(stmt.condition);
    resolve(stmt.thenBranch);
    if (stmt.thenBranch != null)
      resolve(stmt.elseBranch);
    return null;
  }

  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    resolve(stmt.expression);
    return null;
  }

  @Override
  public Void visitReturnStmt(Stmt.Return stmt) {
    if (currentFunction == FunctionType.NONE) {
      Lox.error(stmt.keyword, "Can't return from top-level code.");
    }

    if (stmt.value != null) {
      resolve(stmt.value);
    }
    return null;
  }

  @Override
  public Void visitWhileStmt(Stmt.While stmt) {
    resolve(stmt.condition);
    resolve(stmt.body);
    return null;
  }

  @Override
  public Void visitBinaryExpr(Expr.Binary expr) {
    resolve(expr.left);
    resolve(expr.right);
    return null;
  }

  @Override
  public Void visitCallExpr(Expr.Call expr) {
    resolve(expr.callee);
    for (Expr argument : expr.arguments) {
      resolve(argument);
    }
    return null;
  }

  @Override
  public Void visitGroupingExpr(Expr.Grouping expr) {
    resolve(expr.expression);
    return null;
  }

  @Override
  public Void visitLiteralExpr(Expr.Literal expr) {
    return null;
  }

  @Override
  public Void visitLogicalExpr(Expr.Logical expr) {
    resolve(expr.left);
    resolve(expr.right);
    return null;
  }

  @Override
  public Void visitUnaryExpr(Expr.Unary expr) {
    resolve(expr.right);
    return null;
  }
}
