# test/scanner_test.py

from pylox.error_handler import ErrorHandler  # type: ignore
from pylox.lox_token import Token  # type: ignore
from pylox.scanner import Scanner  # type: ignore
from pylox.token_type import TokenType as TT  # type: ignore


def scan(source: str) -> list[Token]:
    scanner = Scanner(source, ErrorHandler())
    return scanner.scan_tokens()


def test_single_symbol_token() -> None:
    tokens = scan('(')
    assert len(tokens) == 2  # ( + EOF
    assert tokens[0].token_type == TT.LEFT_PAREN
    assert tokens[0].lexeme == '('
    assert tokens[0].literal is None
    assert tokens[0].line == 1
    assert tokens[1].token_type == TT.EOF
