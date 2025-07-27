package jlox.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Java implementation of a tree walk interpreter of Lox.
 */
public class Lox {
  private static final Interpreter interpreter = new Interpreter();

  static boolean hadError = false;
  static boolean hadRuntimeError = false;

  // Entry point of the Lox interpreter
  public static void main(String[] args) throws IOException {
    if (args.length > 1) {
      System.out.println("Usage: jlox [script]");
      System.exit(64);
    } else if (args.length == 1) {
      runFile(args[0]);
    } else {
      runPrompt();
    }
  }

  // Used to run a Lox script
  private static void runFile(String path) throws IOException {
    byte[] bytes = Files.readAllBytes(Paths.get(path));
    run(new String(bytes, Charset.defaultCharset()));
    if (hadError)
      System.exit(65);
    if (hadRuntimeError)
      System.exit(70);
  }

  // Open a Lox REPL
  private static void runPrompt() throws IOException {
    InputStreamReader input = new InputStreamReader(System.in);
    BufferedReader reader = new BufferedReader(input);

    for (;;) {
      System.out.print(">>> ");
      String line = reader.readLine();
      if (line == null)
        break;
      run(line);
      hadError = false;
    }
  }

  // Run Lox code
  private static void run(String source) {
    Scanner scanner = new Scanner(source);
    List<Token> tokens = scanner.scanTokens();

    Parser parser = new Parser(tokens);
    List<Stmt> statements = parser.parse();

    if (hadError)
      return;

    Resolver resolver = new Resolver(interpreter);
    resolver.resolve(statements);

    if (hadError)
      return;

    interpreter.interpret(statements);
  }

  // Error Handling

  // Error for lines
  static void error(int line, String message) {
    report(line, "", message);
  }

  // Error overload for tokens
  static void error(Token token, String message) {
    if (token.type == TokenType.EOF) {
      report(token.line, " at end", message);
    } else {
      report(token.line, "at '" + token.lexeme + "'", message);
    }
  }

  // Error reporting function
  private static void report(int line, String where, String message) {
    System.err.println(String.format("[line %s] Error %s: %s", line, where, message));
    hadError = true;
  }

  // Runtime error reporting code
  static void runtimeError(RuntimeError error) {
    System.err.println(String.format("%s [line %s]", error.getMessage(), error.token.line));
    hadRuntimeError = true;
  }
}