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


def test_keyword_token() -> None:
    tokens = scan('class')
    assert len(tokens) == 2
    assert tokens[0].token_type == TT.CLASS
    assert tokens[0].lexeme == 'class'
    assert tokens[1].token_type == TT.EOF


def test_identifier_token() -> None:
    tokens = scan('fooBar')
    assert len(tokens) == 2
    assert tokens[0].token_type == TT.IDENTIFIER
    assert tokens[0].lexeme == 'fooBar'
    assert tokens[1].token_type == TT.EOF


def test_grouped_expression() -> None:
    tokens = scan('(x + 1);')
    expected_types = [
        TT.LEFT_PAREN,
        TT.IDENTIFIER,
        TT.PLUS,
        TT.NUMBER,
        TT.RIGHT_PAREN,
        TT.SEMICOLON,
        TT.EOF,
    ]
    actual_types = [t.token_type for t in tokens]
    assert actual_types == expected_types


def test_unexpected_char() -> None:
    handler = ErrorHandler()
    scanner = Scanner('@', handler)
    tokens = scanner.scan_tokens()
    assert tokens[-1].token_type == TT.EOF
    assert handler.has_error is True
