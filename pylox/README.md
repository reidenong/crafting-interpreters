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

# Representing code
Key terms:
- Formal grammar: Using a set of atomic pieces as its 'alphabet', we define a (usually infinite) set of 'strings' that are in the grammar. Since these grammars have infinite valid strings, we define them using a set of rules, called productions as they produce strings in the grammar.
- In a Context-free grammar, each rule can be applied regardless of what comes before or after (ie. independent of its surrounding symbols).
- Each production in a context-free grammar has a head and a body. The head is a name and describes what the body generates. eg. (`unary → ( "!" | "-" ) unary | call ;`) 

## Generating the AST
- Since each kind of expression in Lox behaves differently at runtime, the interpreter needs to select a different chunk of code to handle each expression type
- We use the visitor pattern: The interpreter implements a visitor, and thus has `visit_x_expression` implemented. 
  - If we have a `Expr` like `assignment: Expr = Assign(...)`
  - We evaluate it like `result: Object = assignment.accept(interpreter)`
  - Under the hood:
    1. `assignment.accept(interpreter)`
    2. We call `interpreter.visit_assign_expr(self)`
    3. as `Interpreter` implements `Visitor`, it has a method `visit_assign_expr` that performs evaluation specific to Assign expressions
  - This is a double dispatch:
    - First: Runtime type of `Expr` determines which accept is called
    - Second: `accept()` calls the correct `visit_x_expr` on the visitor.
  
- Some typing notes:
  - We use `abc.ABC` to for expression type `Expr` where explicit inheritance is intended.
  - We use `typing.Protocol` for `Visitor` to enable duck typing and to keep things flexible.
  - We also use `dataclasses.dataclass` for the `Expr` subclasses to reduce the boilerplate `__init__` needed otherwise, and also `frozen` enabled us to have immutability.

## Parsing
- Pylox, like jlox implements a Recursive Descent Parser (top-down parser).
- Our parser takes in a sequence of `Tokens` and uses the ordering to build a AST based off the Lox Grammar specification.
- Our implementation is a stateful, single pass parser that maintains a reference to the current token we are at. We move down the Lox grammar, from expressions of lower precedence to those of higher precedence.
```
   * Expression Grammar
   * ======================
   *
   * expression → assignment ;
   * 
   * assignment → (call ".")? IDENTIFIER "=" assignment | logic_or ;
   * 
   * logic_or → logic_and ("or" logic_and)* ;
   * 
   * logic_and → equality ("and" equality)* ;
   * 
   * equality → comparison ( ( "!=" | "==" ) comparison )* ;
   * 
   * comparison → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
   * 
   * term → factor ( ( "-" | "+" ) factor )* ;
   * 
   * factor → unary ( ( "/" | "*" ) unary )* ;
   * 
   * unary → ( "!" | "-" ) unary | call ;
   * 
   * // Includes calls and property access
   * call → primary ( "(" arguments? ")" | "." IDENTIFIER )* ;
   * arguments → expression ( "," expression )* ;
   * 
   * primary → NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")"
   * | IDENTIFIER | "super" "." IDENTIFIER;


   * Statement Grammar
   * ==================
   * 
   * program → declaration* EOF ;
   * 
   * declaration → classDecl | funDecl | varDecl | statement ;
   * 
   * statement → exprStmt | forStmt | ifStmt | printStmt | returnStmt | whileStmt
   * | block ;
   * 
   * block → "{" declaration* "}" ;
   * 
   * forStmt → "for" "(" ( varDecl | exprStmt | ";" )
   * expression? ";"
   * expression ")" statement ;
   * 
   * returnStmt → "return" expression? ";" ;
   * 
   * ifStmt → "if" "(" expression ")" statement ("else" statement)? ;
   * 
   * classDecl → "class" IDENTIFIER ( "<" IDENTIFIER )? "{" function* "}";
   * 
   * funDecl → "fun" function ;
   * function → IDENTIFIER "(" parameters? ")" block ;
   * parameters → IDENTIFIER ( "," IDENTIFIER )* ;
   * 
   * varDecl → "var" IDENTIFIER ( "=" expression)? ";" ;
   * 
   * exprStmt → expression ";" ;
   * printStmt → "print" expression ";" ;
```

## Interpreting
After building the AST, we need to evaluate the AST. This comes in two parts, and we achieve that by way of an interpreter.
1. Expressions
  - Given an expression, we want to evaluate it to some result (and possible with side effects).
  - Our interpreter implements the Visitor Protocol, that is to say it is able to visit each of the different types of the AST nodes. Upon visiting a node, it evaluates its value and any of its associated side effects.
2. Statements
- Statements dont evalute to a value, and instead produce side effects. We can easily create two straightforward statements and their implementations, the expression statement and the print statement.
- There are also 'precedents' for statements, as some statements can fit within other statements. For example, we may want declaration statements to have the highest precedence so that they aren't nested into control flow like `if (some_value) var x = 5;`, where it is not clear where the scope begins and ends.