package jlox.lox;

import java.util.ArrayList;
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
   * declaration → varDecl | statement ;
   * 
   * statement → exprStmt | ifStmt | printStmt | block ;
   * 
   * block → "{" declaration* "}" ;
   * 
   * 
   * ifStmt → "if" "(" expression ")" statement ("else" statement)? ;
   * 
   * varDecl → "var" IDENTIFIER ( "=" expression)? ";" ;
   * 
   * printStmt → expression ";" ;
   * exprStmt → "print" expression ";" ;
   */

  // Parses a declaration at [current]
  private Stmt declaration() {
    try {
      if (match(VAR))
        return varDeclaration();
      return statement();
    } catch (ParseError error) {
      synchronize();
      return null;
    }
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
    if (match(IF))
      return ifStatement();
    if (match(PRINT))
      return printStatement();
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
   * assignment → IDENTIFIER "=" assignment | equality ;
   * 
   * equality → comparison ( ( "!=" | "==" ) comparison )* ;
   * 
   * comparison → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
   * 
   * term → factor ( ( "-" | "+" ) factor )* ;
   * 
   * factor → unary ( ( "/" | "*" ) unary )* ;
   * 
   * unary → ( "!" | "-" ) unary | primary ;
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
    Expr expr = equality(); // Evaluate LHS (into hopefully a identifier)

    if (match(EQUAL)) {
      Token equals = previous();
      Expr value = assignment(); // Evaluate RHS for value recursively

      if (expr instanceof Expr.Variable) { // Update environment
        Token name = ((Expr.Variable) expr).name;
        return new Expr.Assign(name, value);
      }

      error(equals, "Invalid assignment target.");
    }

    return expr; // Propagate r-value
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
    Expr expr = primary();

    while (match(BANG, MINUS)) {
      Token operator = previous();
      Expr right = primary();
      expr = new Expr.Unary(operator, right);
    }
    return expr;
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
