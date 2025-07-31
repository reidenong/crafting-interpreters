# pylox/token_type.py
from enum import StrEnum, auto, unique
from typing import Final  # Type-hint to indicate immutable


@unique  # Ensure no two members have the same value
class TokenType(StrEnum):
    """A string enum which describes the types of tokens."""

    # Single-character tokens.
    LEFT_PAREN = auto()
    RIGHT_PAREN = auto()
    LEFT_BRACE = auto()
    RIGHT_BRACE = auto()
    COMMA = auto()
    DOT = auto()
    MINUS = auto()
    PLUS = auto()
    SEMICOLON = auto()
    SLASH = auto()
    STAR = auto()

    # One or two character tokens.
    BANG = auto()
    BANG_EQUAL = auto()
    EQUAL = auto()
    EQUAL_EQUAL = auto()
    GREATER = auto()
    GREATER_EQUAL = auto()
    LESS = auto()
    LESS_EQUAL = auto()

    # Literals.
    IDENTIFIER = auto()
    STRING = auto()
    NUMBER = auto()

    # Keywords.
    AND = auto()
    CLASS = auto()
    ELSE = auto()
    FALSE = auto()
    FUN = auto()
    FOR = auto()
    IF = auto()
    NIL = auto()
    OR = auto()
    PRINT = auto()
    RETURN = auto()
    SUPER = auto()
    THIS = auto()
    TRUE = auto()
    VAR = auto()
    WHILE = auto()

    # End-of-file
    EOF = auto()


# Maps single-character lexemes to their corresponding token types.
# Used by the scanner to quickly recognize punctuation and operator tokens.
TT = TokenType  # alias
CHAR_TOKEN_MAP: Final[dict[str, TT | dict[str, TT]]] = {
    # Single-character tokens
    '(': TT.LEFT_PAREN,
    ')': TT.RIGHT_PAREN,
    '{': TT.LEFT_BRACE,
    '}': TT.RIGHT_BRACE,
    ',': TT.COMMA,
    '.': TT.DOT,
    '-': TT.MINUS,
    '+': TT.PLUS,
    ';': TT.SEMICOLON,
    '*': TT.STAR,
    '/': TT.SLASH,
    # One or two character tokens (with optional '=' after the first char)
    '!': {'=': TT.BANG_EQUAL, '': TT.BANG},
    '=': {'=': TT.EQUAL_EQUAL, '': TT.EQUAL},
    '<': {'=': TT.LESS_EQUAL, '': TT.LESS},
    '>': {'=': TT.GREATER_EQUAL, '': TT.GREATER},
}

KEYWORD_TOKEN_MAP: Final[dict[str, TT]] = {
    'and': TT.AND,
    'class': TT.CLASS,
    'else': TT.ELSE,
    'false': TT.FALSE,
    'for': TT.FOR,
    'fun': TT.FUN,
    'if': TT.IF,
    'nil': TT.NIL,
    'or': TT.OR,
    'print': TT.PRINT,
    'return': TT.RETURN,
    'super': TT.SUPER,
    'this': TT.THIS,
    'true': TT.TRUE,
    'var': TT.VAR,
    'while': TT.WHILE,
}
