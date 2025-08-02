# pylox/scanner.py
from .error_handler import ErrorHandler
from .lox_token import Token
from .token_type import CHAR_TOKEN_MAP, KEYWORD_TOKEN_MAP
from .token_type import TokenType as TT


class Scanner:
    def __init__(self, source: str, error_handler: ErrorHandler) -> None:
        self.source = source
        self.ehand = error_handler
        self.tokens: list[Token] = []

        self.start = 0
        self.curr = 0
        self.line = 1

    def scan_tokens(self) -> list[Token]:
        while not self.is_at_end():
            # At the beginning of the next lexeme
            self.start = self.curr
            self.scan_token()

        self.tokens.append(Token(TT.EOF, '', None, self.line))
        return self.tokens

    def is_at_end(self):
        return self.curr >= len(self.source)

    def scan_token(self) -> None:
        c = self.advance()

        if c in CHAR_TOKEN_MAP:
            self.add_token(CHAR_TOKEN_MAP[c])
            return

        # Individual cases
        match c:
            case '!':
                if self.match('='):
                    self.add_token(TT.BANG_EQUAL)
                else:
                    self.add_token(TT.BANG)
                return

            case '=':
                if self.match('='):
                    self.add_token(TT.EQUAL_EQUAL)
                else:
                    self.add_token(TT.EQUAL)
                return

            case '<':
                if self.match('='):
                    self.add_token(TT.LESS_EQUAL)
                else:
                    self.add_token(TT.LESS)
                return

            case '>':
                if self.match('='):
                    self.add_token(TT.GREATER_EQUAL)
                else:
                    self.add_token(TT.GREATER)
                return

            case '/':
                if self.match('/'):
                    while self.peek() != '\n' and not self.is_at_end():
                        self.advance()
                else:
                    self.add_token(TT.SLASH)
                return

            case ' ' | '\r' | '\t':
                return

            case '\n':
                self.line += 1
                return

            case '"':
                self.string()
                return

            case _ if c.isdigit():
                self.number()
                return

            case _ if c.isalpha():
                self.identifier()
                return

        self.ehand.error(self.line, 'Unexpected char.')

    def identifier(self) -> None:
        while not self.is_at_end() and (
            self.peek().isalnum() or self.peek() == '_'
        ):
            self.advance()

        text = self.source[self.start : self.curr]
        token_type = KEYWORD_TOKEN_MAP.get(text)
        if token_type:
            self.add_token(token_type)
        else:
            self.add_token(TT.IDENTIFIER)

    def number(self) -> None:
        while not self.is_at_end() and self.peek().isdigit():
            self.advance()

        # Decimal portion
        if self.peek() == '.' and self.peek_next().isdigit():
            self.advance()

            while not self.is_at_end() and self.peek().isdigit():
                self.advance()

        self.add_token(TT.NUMBER, float(self.source[self.start : self.curr]))

    def string(self) -> None:
        while not self.is_at_end() and self.peek() != '"':
            if self.peek() == '\n':
                self.line += 1
            self.advance()

        if self.is_at_end():
            self.ehand.error(self.line, 'Unterminated string.')
            return

        self.advance()
        self.add_token(TT.STRING, self.source[self.start + 1 : self.curr - 1])

    def peek_next(self) -> str:
        """Get next char without consuming."""
        return (
            '\0'
            if (self.curr + 1 >= len(self.source))
            else self.source[self.curr + 1]
        )

    def peek(self) -> str:
        """Get current char without consuming."""
        return '\0' if self.is_at_end() else self.source[self.curr]

    def advance(self) -> str:
        """Consumes next character in source and returns it."""
        res = self.source[self.curr]
        self.curr += 1
        return res

    def add_token(self, type: TT, literal: object = None) -> None:
        """Adds the current token to the list of tokens"""
        text = self.source[self.start : self.curr]
        self.tokens.append(Token(type, text, literal, self.line))

    def match(self, expected_char: str) -> bool:
        """Check if we currently match expected_char, and consume if we do."""
        if self.is_at_end() or self.source[self.curr] != expected_char:
            return False
        self.curr += 1
        return True
