# pylox/scanner.py
from pylox import Lox, Token
from pylox import TokenType as TT
from pylox.token_type import CHAR_TOKEN_MAP


class Scanner:
    def __init__(self, source: str) -> None:
        self.source = source
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
        tt_or_map = CHAR_TOKEN_MAP.get(c)

        if isinstance(tt_or_map, TT):
            self.add_token(tt_or_map)
            return

        if isinstance(tt_or_map, dict):
            for key in tt_or_map:
                if self.match(key):
                    self.add_token(tt_or_map[key])
                    return

        Lox.error(self.line, 'Unexpected char.')

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
