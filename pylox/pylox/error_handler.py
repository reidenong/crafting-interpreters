# pylox/error_reporter.py


class ErrorHandler:
    def __init__(self):
        self.has_error = False

    def error(self, line: int, message: str) -> None:
        self.report(line, '', message)

    def report(self, line: int, where: str, message: str) -> None:
        print(f'[line {line}] Error{where}: {message}')
        self.has_error = True
