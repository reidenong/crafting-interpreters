# pylox/stmt.py

 
from __future__ import annotations # For forward references

from abc import ABC, abstractmethod
from dataclasses import dataclass
from typing import Protocol, TypeVar
              
from .lox_token import Token
              
T = TypeVar('T', covariant=True)

    
from .expr import Expr

class Stmt(ABC):
    @abstractmethod
    def accept(self, visitor: Visitor[T]) -> T: ...


class Visitor(Protocol[T]):
    def visit_expression_stmt(self, stmt: Expression) -> T: ...
    def visit_print_stmt(self, stmt: Print) -> T: ...
    def visit_var_stmt(self, stmt: Var) -> T: ...


@dataclass(frozen=True)
class Expression(Stmt):
    expression: Expr

    def accept(self, visitor: Visitor[T]) -> T:
        return visitor.visit_expression_stmt(self)


@dataclass(frozen=True)
class Print(Stmt):
    expression: Expr

    def accept(self, visitor: Visitor[T]) -> T:
        return visitor.visit_print_stmt(self)


@dataclass(frozen=True)
class Var(Stmt):
    name: Token
    initializer: Expr

    def accept(self, visitor: Visitor[T]) -> T:
        return visitor.visit_var_stmt(self)
