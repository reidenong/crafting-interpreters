# Lox Interpreters

Here are some implementations of the Lox language as described in [Crafting Interpreters](https://craftinginterpreters.com/).

Lox is implemented in the following flavors:
- jLox, a Tree walk interpreter of Lox implemented in Java.

# jLox
Setup
1. Building `jlox`
```
make jlox
```
2. Run the `jlox` REPL
```
make jlox-run
```

Under the hood, there is a need to generate the AST:
```
javac jlox/tool/GenerateAst.java
java jlox.tool.GenerateAst ./jlox/lox/
```

# pyLox

About `pylox`: [link](pylox/README.md)

Running `pylox`
```
python pylox/main.py [optional_file.lox]
```


