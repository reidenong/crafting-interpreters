# pylox/main.py
import sys

from pylox.error_handler import ErrorHandler
from pylox.interpreter import Interpreter
from pylox.parser import Parser
from pylox.scanner import Scanner


class Lox:
    error_handler: ErrorHandler
    interpreter: Interpreter

    def __init__(self) -> None:
        self.error_handler = ErrorHandler()
        self.interpreter = Interpreter()

    def main(self) -> None:
        if len(sys.argv) > 2:
            print('Usage: python3 pylox/main.py [script]')
            sys.exit(64)
        elif len(sys.argv) == 2:
            self.run_file(sys.argv[1])
        else:
            self.run_prompt()

    def run_file(self, src_path: str) -> None:
        with open(src_path, 'r', encoding='utf-8') as file:
            src = file.read()
        self.run(src)

        if self.error_handler.has_error:
            sys.exit(65)
        if self.error_handler.has_runtime_error:
            sys.exit(70)

    def run_prompt(self) -> None:
        while True:
            print('>>> ', end='')
            line = input()
            if not line:
                break
            self.run(line)
            self.error_handler.has_error = False

    def run(self, src: str) -> None:
        scanner = Scanner(src, self.error_handler)
        tokens = scanner.scan_tokens()
        parser = Parser(tokens, self.error_handler)
        expr = parser.parse()

        if self.error_handler.has_error or expr is None:
            sys.exit(65)

        self.interpreter.interpret(expr, self.error_handler)


if __name__ == '__main__':
    lox = Lox()
    lox.main()
