# pylox/generate_ast.py
import sys


class StringBuilder:
    def __init__(self) -> None:
        self.parts: list[str] = []

    def append(self, x: str) -> None:
        self.parts.append(x)

    def get_string(self) -> str:
        return '\n'.join(self.parts)


def main() -> None:
    if len(sys.argv) != 2:
        print('Usage: python generate_ast.py <output_dir>')
        sys.exit(64)

    output_dir = sys.argv[1]
    write_ast_node(
        output_dir,
        'Expr',
        [
            'Binary - left: Expr, operator: Token, right: Expr',
            'Grouping - expression: Expr',
            'Literal - value: object',
            'Unary - operator: Token, right: Expr',
        ],
    )


def write_ast_node(output_dir: str, base_name: str, defns: list[str]) -> None:
    base_path = f'{output_dir}{base_name.lower()}.py'
    sb = StringBuilder()

    sb.append(f'# {base_path}\n')
    sb.append(""" 
from abc import ABC, abstractmethod
from dataclasses import dataclass
from typing import Protocol, TypeVar
              
from .lox_token import Token
              
T = TypeVar('T', covariant=True)\n
    """)

    sb.append(f'class {base_name}:')
    sb.append('\tpass')

    for defn in defns:
        class_name = defn.split('-')[0].strip()
        fields_str = defn.split('-')[1].strip()
        sb.append('\n')
        write_class(sb, base_name, class_name, fields_str)

    with open(base_path, 'w') as fp:
        fp.write(sb.get_string())


def write_class(
    sb: StringBuilder, base_name: str, class_name: str, fields_str: str
) -> None:
    sb.append('@dataclass(frozen=True)')
    sb.append(f'class {class_name}({base_name}):')

    for field in fields_str.split(','):
        sb.append(f'\t{field.strip()}')


if __name__ == '__main__':
    main()
