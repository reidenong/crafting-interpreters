# Pylox

A python implementation of the tree walk interpreter of the language Lox. Here are some of the notes that I have while implementing pylox.

## Scanner
The scanner converts the source code `py src: str` into a list of tokens, which then goes into the next stage, the interpreter.

Key concepts:
- The scanner here is implemented with a stateful, single-pass counter that tracks the position of a pointer as it moves through the source code, and uses many functions that rely on the current position of a pointer to add the current token.
    - Real life alternatives:
      - Scanner Generator tools (eg. Lex) use regular expression rules and finite automata to generate scanners automatically from a high-level specification. Used by GNU, Clang, LLVM etc.
      - Regex based manual tokenization (eg. Python's `py re.Scanner`).
