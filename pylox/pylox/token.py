# pylox/token.py

from dataclasses import dataclass
from typing import Any

from .token_type import TokenType


@dataclass(frozen=True, slots=True)
class Token:
    """A lexical token produced by the scanner."""

    token_type: TokenType
    lexeme: str
    literal: Any
    line: int

    def __str__(self):
        return f'{self.token_type} {self.lexeme} {self.literal}'


# We use slots to allocate a fixed set of attributes instead of a dynamic dict.
