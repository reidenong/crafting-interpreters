package jlox.lox;

import java.util.ArrayList;
import java.util.List;

import static jlox.lox.TokenType.*;

/**
 * Parses a sequence of tokens into an AST.
 * 
 * Implemented as a recursive descent parser.
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
      statements.add(statement());
    }
    return statements;
  }

  /**
   * Statement Grammar
   * 
   * program → statement* EOF ;
   * 
   * statement → exprStmt | printStmt ;
   * 
   * printStmt → expression ";" ;
   * exprStmt → "print" expression ";" ;
   */

  // Parses a statement at [current]
  private Stmt statement() {
    if (match(PRINT))
      return printStatement();
    return expressionStatement();
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
   * expression → equality ;
   * equality → comparison ( ( "!=" | "==" ) comparison )* ;
   * comparison → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
   * term → factor ( ( "-" | "+" ) factor )* ;
   * factor → unary ( ( "/" | "*" ) unary )* ;
   * unary → ( "!" | "-" ) unary | primary ;
   * primary → NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")" ;
   */

  private Expr expression() {
    return equality();
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

  // Check to see if current token has any of the given types.
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
