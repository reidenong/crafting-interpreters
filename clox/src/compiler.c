/*
 * Converts source code into chunks of bytecode.
 *
 * Does parsing and code generation in a single pass.
 */
#include "compiler.h"

#include <stdio.h>
#include <stdlib.h>

#include "common.h"
#include "scanner.h"

typedef struct {
    Token current;
    Token previous;
    bool hadError;
    bool panicMode;  // Prevents error cascades.
} Parser;

Parser parser;  // Single global variable to avoid passing it around.

/*
 * Forwards any error messages from the scanner to the user.
 */
static void errorAt(Token* token, const char* message) {
    if (parser.panicMode) return;  // Suppress subsequent errors
    parser.panicMode = true;
    fprintf(stderr, "[line %d] Error", token->line);

    if (token->type == TOKEN_EOF) {
        fprintf(stderr, " at end");
    } else if (token->type == TOKEN_ERROR) {
        // Nothing
    } else {
        fprintf(stderr, " at '%.*s", token->length, token->start);
    }

    fprintf(stderr, ": %s\n", message);
    parser.hadError = true;
}

static void errorAtCurrent(const char* message) {
    errorAt(&parser.current, message);
}

/*
 * Walks through the token stream, retrieves the next token from the scanner and
 * stores it for later use.
 *
 * Also stores the previous token.
 */
static void advance() {
    parser.previous = parser.current;

    for (;;) {
        parser.current = scanToken();
        if (parser.current.type != TOKEN_ERROR) break;

        errorAtCurrent(parser.current.start);
    }
}

/*
 * Reads the next token while validating its expected type.
 */
static void consume(TokenType type, const char* message) {
    if (parser.current.type == type) {
        advance();
        return;
    }

    errorAtCurrent(message);
}

bool compile(const char* source) {
    initScanner(source);

    parser.hadError = false;
    parser.panicMode = true;

    advance();
    expression();  // Only support compiling a single expression for now.
    consume(TOKEN_EOF, "Expect end of expression.");
    return !parser.hadError;
}