from __future__ import annotations  # for forward references

from abc import ABC, abstractmethod
from dataclasses import dataclass
from typing import Protocol, TypeVar

from .lox_token import Token

T = TypeVar('T', covariant=True)


# Use Protocol instead of ABC for Visitor so it can be duck-typed
class Visitor(Protocol[T]):
    def visit_assign_expr(self, expr: Assign) -> T: ...


# Abstract base class for all expressions
class Expr(ABC):
    @abstractmethod
    def accept(self, visitor: Visitor[T]) -> T: ...


# Concrete expression: Assign
@dataclass(frozen=True)
class Assign(Expr):
    name: Token
    value: Expr

    def accept(self, visitor: Visitor[T]) -> T:
        return visitor.visit_assign_expr(self)
