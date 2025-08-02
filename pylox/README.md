# Pylox

A python implementation of the tree walk interpreter of the language Lox. Here are some of the notes that I have while implementing pylox.

## Scanner
The scanner converts the source code `src: str` into a list of tokens, which then goes into the next stage, the interpreter.

Key concepts:
- The scanner here is implemented with a stateful, single-pass counter that tracks the position of a pointer as it moves through the source code, and uses many functions that rely on the current position of a pointer to add the current token.
    - Real life alternatives:
      - Scanner Generator tools (eg. Lex) use regular expression rules and finite automata to generate scanners automatically from a high-level specification. Used by GNU, Clang, LLVM etc.
      - Regex based manual tokenization (eg. Python's `re.Scanner`).
      - Multistage tokenizers (eg. IDEs like VS Code, Rust compiler) have multiple passes that do things like remove comments & whitespace, then handle macro tokens and interpolation, then token classification.

- In the `scan_token` method, we try to encapsulate the simpler cases with `dict` mappings, but resort to pattern matching with `match-case` for the more complicated cases.

- Maximal Munch (longest match): we always match the longest possible token, eg. resolving `---a` as `-- -a` instead of `- --a`. Multiple tokens may begin with the same prefix, so we try to consume as many characters as possible that still make a valid token. It helps with correctness in misinterpreting longer tokens as multiple shorter ones, while helping the grammar and parsing to remain simple.
