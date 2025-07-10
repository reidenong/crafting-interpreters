# Lox Interpreters

Here are some implementations of the Lox language as described in [Crafting Interpreters](https://craftinginterpreters.com/).

Lox is implemented in the following flavors:
- jLox, a Tree walk interpreter of Lox implemented in Java.

Setup
1. Generating the AST source code.
```
javac jlox/tool/GenerateAst.java
java jlox.tool.GenerateAst ./jlox/lox/
```
2. Building `jlox`
```
make jlox
```
3. Run the `jlox` REPL
```
make jlox-run
```