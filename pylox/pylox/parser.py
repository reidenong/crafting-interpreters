# pylox/parser.py

from __future__ import annotations  # Allows forward references.

from .error_handler import ErrorHandler
from .expr import Binary, Expr, Grouping, Literal, Unary, Variable
from .lox_token import Token
from .stmt import Expression, Print, Stmt, Var
from .token_type import TokenType as TT


class Parser:
    tokens: list[Token]
    curr: int
    eh: ErrorHandler

    def __init__(self, tokens: list[Token], eh: ErrorHandler) -> None:
        self.tokens = tokens
        self.curr = 0
        self.eh = eh

    def parse(self) -> list[Stmt] | None:
        statements = []
        try:
            while not self.is_at_end():
                statements.append(self.declaration())
        except ParseError:
            return None
        return statements

    """
    PARSING STATEMENTS
    """

    def var_declaration(self) -> Stmt:
        """
        var_declaration
        """
        name = self.consume(TT.IDENTIFIER, 'Expect variable name.')

        initializer = None
        if self.match(TT.EQUAL):
            initializer = self.expression()
        self.consume(TT.SEMICOLON, 'Expect ";" after variable declaration.')
        return Var(name, initializer)

    def declaration(self) -> Stmt | None:
        """
        declaration → var_declaration | statement ;
        """
        try:
            if self.match(TT.VAR):
                return self.var_declaration()
            return self.statement()
        except ParseError:
            self.synchronize()
        return None

    def statement(self) -> Stmt:
        """
        statement → print_statement | expression_statement() ;
        """
        if self.match(TT.PRINT):
            return self.print_statement()
        return self.expression_statement()

    def print_statement(self) -> Stmt:
        """
        print_statement → "print" expression ";" ;
        """
        value = self.expression()
        self.consume(TT.SEMICOLON, 'Expect ";" after value.')
        return Print(value)

    def expression_statement(self) -> Stmt:
        """
        expression_statement → expression ";" ;
        """
        value = self.expression()
        self.consume(TT.SEMICOLON, 'Expect ";" after value.')
        return Expression(value)

    """
    PARSING EXPRESSIONS
    """

    def expression(self) -> Expr:
        """
        expression → equality ;
        """
        return self.equality()

    def equality(self) -> Expr:
        """
        equality → comparison ( ( "!=" | "==" ) comparison)* ;
        """
        expr: Expr = self.comparison()

        while self.match(TT.BANG_EQUAL, TT.EQUAL_EQUAL):
            operator: Token = self.previous()
            right: Expr = self.comparison()
            expr = Binary(expr, operator, right)

        return expr

    def comparison(self) -> Expr:
        """
        comparison → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
        """
        expr: Expr = self.term()

        while self.match(TT.GREATER, TT.GREATER_EQUAL, TT.LESS, TT.LESS_EQUAL):
            operator: Token = self.previous()
            right: Expr = self.term()
            expr = Binary(expr, operator, right)

        return expr

    def term(self) -> Expr:
        """
        term  → factor (( "+" | "-" ) factor )* ;
        """
        expr: Expr = self.factor()

        while self.match(TT.PLUS, TT.MINUS):
            operator: Token = self.previous()
            right: Expr = self.factor()
            expr = Binary(expr, operator, right)

        return expr

    def factor(self) -> Expr:
        """
        factor → unary (( "*" | "/" ) unary)* ;
        """
        expr: Expr = self.unary()

        while self.match(TT.STAR, TT.SLASH):
            operator: Token = self.previous()
            right: Expr = self.factor()
            expr = Binary(expr, operator, right)

        return expr

    def unary(self) -> Expr:
        """
        unary → ("!" | "-")* unary | primary ;
        """
        if self.match(TT.BANG, TT.MINUS):
            operator: Token = self.previous()
            right: Expr = self.unary()
            return Unary(operator, right)

        return self.primary()

    def primary(self) -> Expr:
        """
        primary → NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")"  | IDENTIFIER;
        """
        if self.match(TT.FALSE):
            return Literal(False)
        if self.match(TT.TRUE):
            return Literal(True)
        if self.match(TT.NIL):
            return Literal(None)

        if self.match(TT.NUMBER, TT.STRING):
            return Literal(self.previous().literal)

        if self.match(TT.LEFT_PAREN):
            expr: Expr = self.expression()
            self.consume(TT.RIGHT_PAREN, "Expect ')' after expression.")
            return Grouping(expr)

        if self.match(TT.IDENTIFIER):
            return Variable(self.previous())

        raise self.error(self.peek(), 'Expect expression.')

    """
    HELPER FUNCTIONS
    """

    def consume(self, token_type: TT, message: str) -> Token:
        if self.check(token_type):
            return self.advance()
        raise self.error(self.peek(), message)

    def error(self, token: Token, message: str) -> ParseError:
        self.eh.error(token, message)
        return ParseError()

    def match(self, *args: TT) -> bool:
        """Match current token with possibile args, consume if present, return status."""
        for arg in args:
            if self.check(arg):
                self.advance()
                return True
        return False

    def check(self, typ: TT) -> bool:
        """Check if current token matches, but never consumes."""
        if self.is_at_end():
            return False
        return self.peek().token_type == typ

    def advance(self) -> Token:
        if not self.is_at_end():
            self.curr += 1
        return self.previous()

    def is_at_end(self) -> bool:
        return self.peek().token_type == TT.EOF

    def peek(self) -> Token:
        return self.tokens[self.curr]

    def previous(self) -> Token:
        return self.tokens[self.curr - 1]

    def synchronize(self) -> None:
        self.advance()

        while not self.is_at_end():
            if self.previous().token_type == TT.SEMICOLON:
                return

            if self.peek().token_type in [
                TT.CLASS,
                TT.FUN,
                TT.VAR,
                TT.FOR,
                TT.IF,
                TT.WHILE,
                TT.PRINT,
                TT.RETURN,
            ]:
                return
            self.advance()


class ParseError(RuntimeError):
    pass
