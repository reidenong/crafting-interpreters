# pylox/lox_runtime_error.py

from .lox_token import Token


class LoxRuntimeError(RuntimeError):
    token: Token

    def __init__(self, token: Token, message: str) -> None:
        super(message)
        self.token = token
