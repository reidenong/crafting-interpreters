# pylox/main.py
import sys

from pylox import Scanner


class Lox:
    def __init__(self) -> None:
        self.had_error = False

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

        if self.had_error:
            sys.exit(65)

    def run_prompt(self) -> None:
        while True:
            print('>>> ', end='')
            line = input()
            if not line:
                break
            self.run(line)
            self.had_error = False

    def run(self, src: str) -> None:
        scanner = Scanner(src)
        tokens = scanner.scan_tokens()

        for token in tokens:
            print(token)

    """
    Error Handling
    """

    @staticmethod
    def error(line: int, message: str) -> None:
        Lox.report(line, '', message)

    @staticmethod
    def report(line: int, where: str, message: str) -> None:
        print(f'[line {line}] Error {where}: {str}')


if __name__ == '__main__':
    lox = Lox()
    lox.main()
