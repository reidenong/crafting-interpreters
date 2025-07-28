package jlox.lox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static jlox.lox.TokenType.*;

/**
 * Parses a sequence of tokens into an AST.
 * 
 * Implemented as a recursive descent parser.
 * We recursively parse sequences of Tokens into AST nodes, building
 * the tree as we go along.
 */
class Parser {
  private final List<Token> tokens;
  private int current = 0;

  Parser(List<Token> tokens) {
    this.tokens = tokens;
  }

  private static class ParseError extends RuntimeException {
  }

  /**
   * Parses a list of tokens into a list of statements.
   * 
   * @return A List of statements that make up the source
   */
  List<Stmt> parse() {
    List<Stmt> statements = new ArrayList<>();
    while (!isAtEnd()) {
      statements.add(declaration());
    }
    return statements;
  }

  /**
   * Statement Grammar
   * 
   * program → declaration* EOF ;
   * 
   * declaration → classDecl | funDecl | varDecl | statement ;
   * 
   * statement → exprStmt | forStmt | ifStmt | printStmt | returnStmt | whileStmt
   * | block ;
   * 
   * block → "{" declaration* "}" ;
   * 
   * forStmt → "for" "(" ( varDecl | exprStmt | ";" )
   * expression? ";"
   * expression ")" statement ;
   * 
   * returnStmt → "return" expression? ";" ;
   * 
   * ifStmt → "if" "(" expression ")" statement ("else" statement)? ;
   * 
   * classDecl → "class" IDENTIFIER ( "<" IDENTIFIER )? "{" function* "}";
   * 
   * funDecl → "fun" function ;
   * function → IDENTIFIER "(" parameters? ")" block ;
   * parameters → IDENTIFIER ( "," IDENTIFIER )* ;
   * 
   * varDecl → "var" IDENTIFIER ( "=" expression)? ";" ;
   * 
   * printStmt → expression ";" ;
   * exprStmt → "print" expression ";" ;
   */

  // Parses a declaration at [current]
  private Stmt declaration() {
    try {
      if (match(CLASS))
        return classDeclaration();
      if (match(FUN))
        return function("function");
      if (match(VAR))
        return varDeclaration();
      return statement();
    } catch (ParseError error) {
      synchronize();
      return null;
    }
  }

  private Stmt classDeclaration() {
    Token name = consume(IDENTIFIER, "Expect class name.");

    Expr.Variable superclass = null;
    if (match(LESS)) {
      consume(IDENTIFIER, "Expect superclass name.");
      superclass = new Expr.Variable(previous());
    }

    consume(LEFT_BRACE, "Expect '{' before class body.");

    List<Stmt.Function> methods = new ArrayList<>();
    while (!check(RIGHT_BRACE) && !isAtEnd()) {
      methods.add(function("method"));
    }

    consume(RIGHT_BRACE, "Expect '}' after class body.");
    return new Stmt.Class(name, superclass, methods);
  }

  private Stmt.Function function(String kind) {
    Token name = consume(IDENTIFIER, String.format("Expect %s name.", kind));
    consume(LEFT_PAREN, String.format("Expect '(' after %s name.", kind));
    List<Token> parameters = new ArrayList<>();
    if (!check(RIGHT_PAREN)) {
      do {
        if (parameters.size() >= 255) {
          error(peek(), "Can't have more than 255 parameters.");
        }

        parameters.add(consume(IDENTIFIER, "Expect parameter name."));
      } while (match(COMMA));
    }
    consume(RIGHT_PAREN, "Expect ')' after parameters.");

    consume(LEFT_BRACE, String.format("Expect '{' before %s body.", kind));
    List<Stmt> body = block();
    return new Stmt.Function(name, parameters, body);
  }

  private Stmt varDeclaration() {
    Token name = consume(IDENTIFIER, "Expect variable name.");

    Expr initializer = null;
    if (match(EQUAL)) {
      initializer = expression();
    }

    consume(SEMICOLON, "Expect ';' after variable declaration.");
    return new Stmt.Var(name, initializer);
  }

  // Parses a statement at [current]
  private Stmt statement() {
    if (match(FOR))
      return forStatement();
    if (match(IF))
      return ifStatement();
    if (match(PRINT))
      return printStatement();
    if (match(RETURN))
      return returnStatement();
    if (match(WHILE))
      return whileStatement();
    if (match(LEFT_BRACE))
      return new Stmt.Block(block());
    return expressionStatement();
  }

  private List<Stmt> block() {
    List<Stmt> statements = new ArrayList<>();

    while (!check(RIGHT_BRACE) && !isAtEnd()) {
      statements.add(declaration());
    }

    consume(RIGHT_BRACE, "Expect '}' after block.");
    return statements;
  }

  // Implement parsing of for-loops as syntactic sugar of a while loop
  // We take in the init, condition, increment and body of the for loop, and then
  // step by step we convert the for loop syntax into a while loop using scoping
  // with blocks.
  private Stmt forStatement() {

    // Extract initializer
    consume(LEFT_PAREN, "Expect '(' after 'for'.");
    Stmt initializer;
    if (match(SEMICOLON)) {
      initializer = null;
    } else if (match(VAR)) {
      initializer = varDeclaration();
    } else {
      initializer = expressionStatement();
    }

    // Extract condition
    Expr condition = null;
    if (!check(SEMICOLON))
      condition = expression();
    consume(SEMICOLON, "Expect ';' after loop condition");

    // Extract increment
    Expr increment = null;
    if (!check(RIGHT_PAREN))
      increment = expression();
    consume(RIGHT_PAREN, "Expect ')' after for clauses");

    // Extract body
    Stmt body = statement();

    // Integrate increment at the end of the body.
    if (increment != null) {
      body = new Stmt.Block(
          Arrays.asList(body, new Stmt.Expression(increment)));
    }

    // Integrate condition as a while loop with the body.
    if (condition == null)
      condition = new Expr.Literal(true);
    body = new Stmt.While(condition, body);

    // Add initializer to run before the body.
    if (initializer != null) {
      body = new Stmt.Block(Arrays.asList(initializer, body));
    }

    return body;
  }

  private Stmt ifStatement() {
    consume(LEFT_PAREN, "Expect '(' after if.");
    Expr condition = expression();
    consume(RIGHT_PAREN, "Expect ')' after if.");

    Stmt thenBranch = statement();
    Stmt elseBranch = null;
    if (match(ELSE))
      elseBranch = statement();

    return new Stmt.If(condition, thenBranch, elseBranch);
  }

  private Stmt printStatement() {
    Expr value = expression();
    consume(SEMICOLON, "Expect ';' after value.");
    return new Stmt.Print(value);
  }

  private Stmt returnStatement() {
    Token keyword = previous();
    Expr value = null;

    // Check if we are returning an expression.
    if (!check(SEMICOLON))
      value = expression();

    consume(SEMICOLON, "Expect ';' after return value.");
    return new Stmt.Return(keyword, value);
  }

  private Stmt whileStatement() {
    consume(LEFT_PAREN, "Expect '(' after if.");
    Expr condition = expression();
    consume(RIGHT_PAREN, "Expect ')' after if.");

    Stmt body = statement();
    return new Stmt.While(condition, body);
  }

  private Stmt expressionStatement() {
    Expr expr = expression();
    consume(SEMICOLON, "Expect ';' after value.");
    return new Stmt.Expression(expr);
  }

  /*
   * Expression Grammar
   *
   * expression → assignment ;
   * 
   * assignment → (call ".")? IDENTIFIER "=" assignment | logic_or ;
   * 
   * logic_or → logic_and ("or" logic_and)* ;
   * 
   * logic_and → equality ("and" equality)* ;
   * 
   * equality → comparison ( ( "!=" | "==" ) comparison )* ;
   * 
   * comparison → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
   * 
   * term → factor ( ( "-" | "+" ) factor )* ;
   * 
   * factor → unary ( ( "/" | "*" ) unary )* ;
   * 
   * unary → ( "!" | "-" ) unary | call ;
   * 
   * // Includes calls and property access
   * call → primary ( "(" arguments? ")" | "." IDENTIFIER )* ;
   * arguments → expression ( "," expression )* ;
   * 
   * primary → NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")"
   * | IDENTIFIER;
   */

  private Expr expression() {
    return assignment();
  }

  // Assignment is right associative.
  // We parse LHS as a higher precedence expression, and then recursively evalute
  // the RHS for value. At the end of the parsing, we then check if l-value is a
  // valid assignment target and then assign it the relevant r-value (from
  // recursive evaluation).
  private Expr assignment() {
    Expr expr = or(); // Evaluate LHS (into hopefully a identifier)

    if (match(EQUAL)) {
      Token equals = previous();
      Expr value = assignment(); // Evaluate RHS for value recursively

      if (expr instanceof Expr.Variable) { // Update environment
        Token name = ((Expr.Variable) expr).name;
        return new Expr.Assign(name, value);
      } else if (expr instanceof Expr.Get) {
        Expr.Get get = (Expr.Get) expr;
        return new Expr.Set(get.object, get.name, value);
      }

      error(equals, "Invalid assignment target.");
    }

    return expr; // Propagate r-value
  }

  // Parse a statement at the logical OR.
  private Expr or() {
    Expr expr = and();

    while (match(OR)) {
      Token operator = previous();
      Expr right = and();
      expr = new Expr.Logical(expr, operator, right);
    }

    return expr;
  }

  // Parse a statement at the logical AND level.
  private Expr and() {
    Expr expr = equality();

    while (match(AND)) {
      Token operator = previous();
      Expr right = equality();
      expr = new Expr.Logical(expr, operator, right);
    }

    return expr;
  }

  private Expr equality() {
    Expr expr = comparison(); // Take the current symbol

    while (match(BANG_EQUAL, EQUAL_EQUAL)) { // Find all equality delimiters
      Token operator = previous(); // Get current operator
      Expr right = comparison(); // Get the right most symbol
      expr = new Expr.Binary(expr, operator, right); // Recursively nest the new symbol
    }
    return expr; // Return nested symbol
  }

  private Expr comparison() {
    Expr expr = term();

    while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
      Token operator = previous();
      Expr right = term();
      expr = new Expr.Binary(expr, operator, right);
    }
    return expr;
  }

  private Expr term() {
    Expr expr = factor();

    while (match(MINUS, PLUS)) {
      Token operator = previous();
      Expr right = factor();
      expr = new Expr.Binary(expr, operator, right);
    }
    return expr;
  }

  private Expr factor() {
    Expr expr = unary();

    while (match(SLASH, STAR)) {
      Token operator = previous();
      Expr right = unary();
      expr = new Expr.Binary(expr, operator, right);
    }
    return expr;
  }

  private Expr unary() {
    if (match(BANG, MINUS)) {
      Token operator = previous();
      Expr right = unary();
      return new Expr.Unary(operator, right);
    }
    return call();
  }

  private Expr call() {
    Expr expr = primary();

    while (true) {
      if (match(LEFT_PAREN)) {
        expr = finishCall(expr);
      } else if (match(DOT)) {
        Token name = consume(IDENTIFIER, "Expect property name after '.'");
        expr = new Expr.Get(expr, name);
      } else {
        break;
      }
    }
    return expr;
  }

  private Expr finishCall(Expr callee) {
    List<Expr> arguments = new ArrayList<>();
    if (!check(RIGHT_PAREN)) {
      do {
        if (arguments.size() >= 255) {
          error(peek(), "Can't have more than 255 arguments.");
        }
        arguments.add(expression());
      } while (match(COMMA));
    }

    // Store the closing paren so we have a location
    // to report a runtime error call.
    Token paren = consume(RIGHT_PAREN, "Expect ')' after arguments.");

    return new Expr.Call(callee, paren, arguments);
  }

  private Expr primary() {
    if (match(FALSE))
      return new Expr.Literal(false);
    if (match(TRUE))
      return new Expr.Literal(true);
    if (match(NIL))
      return new Expr.Literal(null);

    if (match(NUMBER, STRING))
      return new Expr.Literal(previous().literal);

    if (match(THIS))
      return new Expr.This(previous());

    // Variable expression,, ie. getting value of a variable
    if (match(IDENTIFIER))
      return new Expr.Variable(previous());

    if (match(LEFT_PAREN)) {
      Expr expr = expression();
      consume(RIGHT_PAREN, "Expect ')' after expression.");
      return new Expr.Grouping(expr);
    }
    throw error(peek(), "Expect expression.");
  }

  // ==============
  // Parsing utils.
  // ==============

  // Consume current token if present, else throw error
  private Token consume(TokenType type, String message) {
    if (check(type))
      return advance();
    throw error(peek(), message);
  }

  // Check to see if current token has any of the given types. Consumes if yes.
  private boolean match(TokenType... types) {
    for (TokenType type : types) {
      if (check(type)) {
        advance(); // Consumes token
        return true;
      }
    }
    return false;
  }

  // Check if current token is of a given type.
  private boolean check(TokenType type) {
    if (isAtEnd())
      return false;
    return peek().type == type;
  }

  // Describes a Parsing error
  private ParseError error(Token token, String message) {
    Lox.error(token, message);
    return new ParseError();
  }

  // Synchronizes our parser. Call after catching a ParseError.
  // Discards tokens until we have found a statement boundary.
  private void synchronize() {
    advance();

    while (!isAtEnd()) {
      if (previous().type == SEMICOLON)
        return;

      switch (peek().type) {
        case CLASS:
        case FUN:
        case VAR:
        case FOR:
        case IF:
        case WHILE:
        case PRINT:
        case RETURN:
          return;
        default:
          break;
      }
      advance();
    }
  }

  // Primitive operations.

  // Consume current token and return it.
  private Token advance() {
    if (!isAtEnd())
      current++;
    return previous();
  }

  // Check if we are at the end.
  private boolean isAtEnd() {
    return peek().type == EOF;
  }

  // Look at current token, but do not consume.
  private Token peek() {
    return tokens.get(current);
  }

  // Look at previous token.
  private Token previous() {
    return tokens.get(current - 1);
  }
}
