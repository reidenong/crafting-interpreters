# test_parser.py


from pylox.error_handler import ErrorHandler
from pylox.expr import Binary, Grouping, Literal, Unary
from pylox.lox_token import Token
from pylox.parser import Parser
from pylox.token_type import TokenType as TT


def make_token(token_type, lexeme=None, literal=None, line=1):
    return Token(token_type, lexeme or str(token_type), literal, line)


def parse_expr(tokens):
    eh = ErrorHandler()
    tokens = tokens + [make_token(TT.EOF)]  # Always add EOF
    parser = Parser(tokens, eh)
    return parser.parse()


def test_literal_number():
    expr = parse_expr([make_token(TT.NUMBER, '123', 123)])
    assert isinstance(expr, Literal)
    assert expr.value == 123


def test_literal_string():
    expr = parse_expr([make_token(TT.STRING, '"hello"', 'hello')])
    assert isinstance(expr, Literal)
    assert expr.value == 'hello'


def test_unary_minus():
    expr = parse_expr([make_token(TT.MINUS), make_token(TT.NUMBER, '5', 5)])
    assert isinstance(expr, Unary)
    assert expr.operator.token_type == TT.MINUS
    assert isinstance(expr.right, Literal)
    assert expr.right.value == 5


def test_binary_addition():
    expr = parse_expr(
        [
            make_token(TT.NUMBER, '1', 1),
            make_token(TT.PLUS),
            make_token(TT.NUMBER, '2', 2),
        ]
    )
    assert isinstance(expr, Binary)
    assert isinstance(expr.left, Literal)
    assert isinstance(expr.right, Literal)
    assert expr.left.value == 1
    assert expr.right.value == 2
    assert expr.operator.token_type == TT.PLUS


def test_grouping():
    expr = parse_expr(
        [
            make_token(TT.LEFT_PAREN),
            make_token(TT.NUMBER, '1', 1),
            make_token(TT.RIGHT_PAREN),
        ]
    )
    assert isinstance(expr, Grouping)
    assert isinstance(expr.expression, Literal)
    assert expr.expression.value == 1


def test_equality_expression():
    expr = parse_expr(
        [
            make_token(TT.NUMBER, '3', 3),
            make_token(TT.EQUAL_EQUAL),
            make_token(TT.NUMBER, '4', 4),
        ]
    )
    assert isinstance(expr, Binary)
    assert expr.operator.token_type == TT.EQUAL_EQUAL


def test_precedence():
    expr = parse_expr(
        [
            make_token(TT.NUMBER, '1', 1),
            make_token(TT.PLUS),
            make_token(TT.NUMBER, '2', 2),
            make_token(TT.STAR),
            make_token(TT.NUMBER, '3', 3),
        ]
    )
    # Should parse as 1 + (2 * 3), i.e., Binary(Literal(1), PLUS, Binary(Literal(2), STAR, Literal(3)))
    assert isinstance(expr, Binary)
    assert expr.operator.token_type == TT.PLUS
    assert isinstance(expr.right, Binary)
    assert expr.right.operator.token_type == TT.STAR
