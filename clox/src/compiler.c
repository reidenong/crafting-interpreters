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

Parser parser;          // Single global variable to avoid passing it around.
Chunk* compilingChunk;  // Global chunk pointer.

static Chunk* currentChunk() { return compilingChunk; }

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

/*
 * Helper functions to add bytes to the chunk.
 */
static void emitByte(uint8_t byte) {
    writeChunk(currentChunk(), byte, parser.previous.line);
}

static void emitBytes(uint8_t byte1, uint8_t byte2) {
    emitByte(byte1);
    emitByte(byte2);
}

/*
 * Add constant to current chunk's value array and return the index.
 */
static uint8_t makeConstant(Value value) {
    int constant = addConstant(currentChunk(), value);
    if (constant > UINT8_MAX) {  // Make sure we don't have too many constants.
        error("Too many constants in one chunk.");
        return 0;
    }
    return (uint8_t)constant;
}

static void emitConstant(Value value) {
    emitBytes(OP_CONSTANT, makeConstant(value));
}

static void emitReturn() { emitByte(OP_RETURN); }

static void endCompiler() { emitReturn(); }

/*
 * Compiles an unary expression.
 */
static void unary() {
    TokenType operatorType = parser.previous.type;

    // Compile the operand.
    expression();

    // Emit the operator instruction.
    switch (operatorType) {
        case TOKEN_MINUS:
            emitByte(OP_NEGATE);
            break;
        default:
            return;  // Unreachable;
    }
}

/*
 * Compiles a grouping expression.
 */
static void grouping() {
    expression();
    consume(TOKEN_RIGHT_PAREN, "Expect ')' after expression.");
    // assume open bracket has been consumed
}

/*
 * Compiles a number literal.
 */
static void number() {
    double value = strtod(parser.previous.start, NULL);
    // strtod: string to double, stops automatically when it reaches the first
    // non-numeric character
    emitConstant(value);
}

static void expression() {}

bool compile(const char* source, Chunk* chunk) {
    initScanner(source);
    compilingChunk = chunk;

    parser.hadError = false;
    parser.panicMode = true;

    advance();
    expression();  // Only support compiling a single expression for now.
    consume(TOKEN_EOF, "Expect end of expression.");
    endCompiler();  // Finish compiling code; send OP_RETURN
    return !parser.hadError;
}