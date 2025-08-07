# pylox/expr.py

 
from __future__ import annotations # For forward references

from abc import ABC, abstractmethod
from dataclasses import dataclass
from typing import Protocol, TypeVar
              
from .lox_token import Token
              
T = TypeVar('T', covariant=True)

    
class Expr(ABC):
    @abstractmethod
    def accept(self, visitor: Visitor[T]) -> T: ...


class Visitor(Protocol[T]):
    def visit_binary_expr(self, expr: Binary) -> T: ...
    def visit_grouping_expr(self, expr: Grouping) -> T: ...
    def visit_literal_expr(self, expr: Literal) -> T: ...
    def visit_unary_expr(self, expr: Unary) -> T: ...
    def visit_variable_expr(self, expr: Variable) -> T: ...


@dataclass(frozen=True)
class Binary(Expr):
    left: Expr
    operator: Token
    right: Expr

    def accept(self, visitor: Visitor[T]) -> T:
        return visitor.visit_binary_expr(self)


@dataclass(frozen=True)
class Grouping(Expr):
    expression: Expr

    def accept(self, visitor: Visitor[T]) -> T:
        return visitor.visit_grouping_expr(self)


@dataclass(frozen=True)
class Literal(Expr):
    value: object

    def accept(self, visitor: Visitor[T]) -> T:
        return visitor.visit_literal_expr(self)


@dataclass(frozen=True)
class Unary(Expr):
    operator: Token
    right: Expr

    def accept(self, visitor: Visitor[T]) -> T:
        return visitor.visit_unary_expr(self)


@dataclass(frozen=True)
class Variable(Expr):
    name: Token

    def accept(self, visitor: Visitor[T]) -> T:
        return visitor.visit_variable_expr(self)
