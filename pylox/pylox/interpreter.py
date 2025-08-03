# pylox/interpreter.py

from typing import cast

from .error_handler import ErrorHandler
from .expr import Binary, Expr, Grouping, Literal, Unary
from .expr import Visitor as ExprVisitor
from .lox_runtime_error import LoxRuntimeError
from .lox_token import Token
from .token_type import TokenType as TT


class Interpreter(ExprVisitor[object]):
    def interpret(self, expression: Expr, eh: ErrorHandler) -> None:
        try:
            value = self.evaluate(expression)
            print(self.stringify(value))
        except LoxRuntimeError as e:
            eh.runtime_error(e)

    def visit_literal_expr(self, expr: Literal) -> object:
        return expr.value

    def visit_grouping_expr(self, expr: Grouping) -> object:
        return self.evaluate(expr.expression)

    def visit_unary_expr(self, expr: Unary) -> object:
        right = self.evaluate(expr.right)

        match expr.operator.token_type:
            case TT.MINUS:
                self.check_number_operand(expr.operator, right)
                return -cast(float, right)

            case TT.BANG:
                return not self.is_truthy(right)

        return None  # Never reaches.

    def visit_binary_expr(self, expr: Binary) -> object:
        left = self.evaluate(expr.left)
        right = self.evaluate(expr.right)

        match expr.operator.token_type:
            case TT.MINUS:
                self.check_number_operand(expr.operator, left, right)
                return cast(float, left) - cast(float, right)
            case TT.SLASH:
                self.check_number_operand(expr.operator, left, right)
                return cast(float, left) / cast(float, right)
            case TT.STAR:
                self.check_number_operand(expr.operator, left, right)
                return cast(float, left) * cast(float, right)
            case TT.PLUS:
                if isinstance(left, float) and isinstance(right, float):
                    return float(left) + float(right)
                if isinstance(left, str) and isinstance(right, str):
                    return str(left) + str(right)
            case TT.GREATER:
                self.check_number_operand(expr.operator, left, right)
                return cast(float, left) > cast(float, right)
            case TT.GREATER_EQUAL:
                self.check_number_operand(expr.operator, left, right)
                return cast(float, left) >= cast(float, right)
            case TT.LESS:
                self.check_number_operand(expr.operator, left, right)
                return cast(float, left) < cast(float, right)
            case TT.LESS_EQUAL:
                self.check_number_operand(expr.operator, left, right)
                return cast(float, left) <= cast(float, right)
            case TT.BANG_EQUAL:
                return left != right
            case TT.EQUAL_EQUAL:
                return left == right

        return None  # Never reaches

    """
    HELPER FUNCTIONS
    """

    def stringify(self, obj: object) -> str:
        if obj is None:
            return 'nil'
        if isinstance(obj, float):
            if int(obj) == float(obj):
                return str(int(obj))
            return str(obj)
        return repr(obj)

    def check_number_operand(self, operator: Token, *operands: object) -> None:
        if not all(isinstance(operand, float) for operand in operands):
            raise LoxRuntimeError(operator, 'Operand(s) must be numbers.')

    def is_truthy(self, obj: object) -> bool:
        """
        Lox follows Ruby: False and None are falsey, everything else is truthy.
        """
        if obj is None:
            return False
        if isinstance(obj, bool):
            return bool(obj)
        return True

    def evaluate(self, expr: Expr) -> object:
        return expr.accept(self)
