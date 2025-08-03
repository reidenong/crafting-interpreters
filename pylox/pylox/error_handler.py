# pylox/error_reporter.py

from .lox_runtime_error import LoxRuntimeError
from .lox_token import Token
from .token_type import TokenType as TT


class ErrorHandler:
    has_error: bool
    has_runtime_error: bool

    def __init__(self) -> None:
        self.has_error = False
        self.has_runtime_error = False

    def runtime_error(self, e: LoxRuntimeError) -> None:
        print(f'{str(e)} [line {e.token.line}]')
        self.has_runtime_error = True

    def error(self, token_or_line: Token | int, message: str) -> None:
        # line
        if isinstance(token_or_line, int):
            self.report(token_or_line, '', message)

        # Token
        if isinstance(token_or_line, Token):
            tk = token_or_line
            if tk.token_type == TT.EOF:
                self.report(tk.line, ' at end', message)
            else:
                self.report(tk.line, f" at '{tk.lexeme}'", message)

    def report(self, line: int, where: str, message: str) -> None:
        print(f'[line {line}] Error{where}: {message}')
        self.has_error = True
