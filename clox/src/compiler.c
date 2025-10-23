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

#ifdef DEBUG_PRINT_CODE
#include "debug.h"
#endif

typedef struct {
    Token current;
    Token previous;
    bool hadError;
    bool panicMode;  // Prevents error cascades.
} Parser;

/*
 * Defines the precedence of several operations
 *
 * When we parse at a low level of precedence, we may include items of higher
 * precedence.
 *
 * Conversely, when we parse at a higher level of precedence, we don't include
 * items of higher precedence, and stop there
 *
 * This allows us to correctly parse expressions like "-a.b + c" into -(a.b) + c
 * as it just parses the -a.b and stops there instead of consuming the entire
 * expression
 */
typedef enum {
    PREC_NONE,
    PREC_ASSIGNMENT,  // =
    PREC_OR,          // or
    PREC_AND,         // and
    PREC_EQUALITY,    // == !=
    PREC_COMPARISON,  // < > <= >=
    PREC_TERM,        // + -
    PREC_FACTOR,      // * /
    PREC_UNARY,       // ! -
    PREC_CALL,        // . ()
    PREC_PRIMARY
} Precedence;

/*
 * Defines the core lookup table for the Pratt parser.
 *
 * Tells the parser how to handle each token type in an expression, depending on
 * where that token appears and what its operator precedence is.
 */
typedef void (*ParseFn)();  // Function pointer for parsing functions

typedef struct {
    ParseFn prefix;  // Function to call if the token appears at the start
    ParseFn infix;  // Function to call if the token appears between expressions
    Precedence precedence;
} ParseRule;

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

static void endCompiler() {
    emitReturn();

    #ifdef DEBUG_PRINT_CODE
    /*
     * If our code compiles successfully, we can print the chunk in debug mode.
     */
    if (!parser.hadError) {
        disassembleChunk(currentChunk(), "code");
    }
    #endif
}

// Forward declarations.
static void expression();
static ParseRule* getRule(TokenType type);
static void parsePrecedence(Precedence precedence);

/*
 * Compiles a binary expression.
 */
static void binary() {
    // Based on the operatorType, we determine which precedence we should
    // continue at
    TokenType operatorType = parser.previous.type;
    ParseRule* rule = getRule(operatorType);
    parsePrecedence((Precedence)(rule->precedence + 1));

    switch (operatorType) {
        case TOKEN_PLUS:
            emitByte(OP_ADD);
            break;
        case TOKEN_MINUS:
            emitByte(OP_SUBTRACT);
            break;
        case TOKEN_STAR:
            emitByte(OP_MULTIPLY);
            break;
        case TOKEN_SLASH:
            emitByte(OP_DIVIDE);
            break;
        default:
            return;
    }
}

/*
 * Pratt Parsing table
 *
 * Tells us the respective parsing function to call when we encounter a specific
 * type of token, as well as its precedence.
 */
ParseRule rules[] = {
    [TOKEN_LEFT_PAREN] = {grouping, NULL, PREC_NONE},
    [TOKEN_RIGHT_PAREN] = {NULL, NULL, PREC_NONE},
    [TOKEN_LEFT_BRACE] = {NULL, NULL, PREC_NONE},
    [TOKEN_RIGHT_BRACE] = {NULL, NULL, PREC_NONE},
    [TOKEN_COMMA] = {NULL, NULL, PREC_NONE},
    [TOKEN_DOT] = {NULL, NULL, PREC_NONE},
    [TOKEN_MINUS] = {unary, binary, PREC_TERM},
    [TOKEN_PLUS] = {NULL, binary, PREC_TERM},
    [TOKEN_SEMICOLON] = {NULL, NULL, PREC_NONE},
    [TOKEN_SLASH] = {NULL, binary, PREC_FACTOR},
    [TOKEN_STAR] = {NULL, binary, PREC_FACTOR},
    [TOKEN_BANG] = {NULL, NULL, PREC_NONE},
    [TOKEN_BANG_EQUAL] = {NULL, NULL, PREC_NONE},
    [TOKEN_EQUAL] = {NULL, NULL, PREC_NONE},
    [TOKEN_EQUAL_EQUAL] = {NULL, NULL, PREC_NONE},
    [TOKEN_GREATER] = {NULL, NULL, PREC_NONE},
    [TOKEN_GREATER_EQUAL] = {NULL, NULL, PREC_NONE},
    [TOKEN_LESS] = {NULL, NULL, PREC_NONE},
    [TOKEN_LESS_EQUAL] = {NULL, NULL, PREC_NONE},
    [TOKEN_IDENTIFIER] = {NULL, NULL, PREC_NONE},
    [TOKEN_STRING] = {NULL, NULL, PREC_NONE},
    [TOKEN_NUMBER] = {number, NULL, PREC_NONE},
    [TOKEN_AND] = {NULL, NULL, PREC_NONE},
    [TOKEN_CLASS] = {NULL, NULL, PREC_NONE},
    [TOKEN_ELSE] = {NULL, NULL, PREC_NONE},
    [TOKEN_FALSE] = {NULL, NULL, PREC_NONE},
    [TOKEN_FOR] = {NULL, NULL, PREC_NONE},
    [TOKEN_FUN] = {NULL, NULL, PREC_NONE},
    [TOKEN_IF] = {NULL, NULL, PREC_NONE},
    [TOKEN_NIL] = {NULL, NULL, PREC_NONE},
    [TOKEN_OR] = {NULL, NULL, PREC_NONE},
    [TOKEN_PRINT] = {NULL, NULL, PREC_NONE},
    [TOKEN_RETURN] = {NULL, NULL, PREC_NONE},
    [TOKEN_SUPER] = {NULL, NULL, PREC_NONE},
    [TOKEN_THIS] = {NULL, NULL, PREC_NONE},
    [TOKEN_TRUE] = {NULL, NULL, PREC_NONE},
    [TOKEN_VAR] = {NULL, NULL, PREC_NONE},
    [TOKEN_WHILE] = {NULL, NULL, PREC_NONE},
    [TOKEN_ERROR] = {NULL, NULL, PREC_NONE},
    [TOKEN_EOF] = {NULL, NULL, PREC_NONE},
};

/*
 * Compiles an unary expression.
 */
static void unary() {
    TokenType operatorType = parser.previous.type;

    // Compile the operand.
    parsePrecedence(PREC_UNARY);
    // we parse at the same level of precedence to allow things like !!x

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
 * The core recursive engine of the Pratt parser.
 *
 * Parses an expression whose operators have at least the given precedence
 * level.
 *
 * This ensures that higher-precedence operators bind tighter than
 * lower-precedence ones. It reads an expression via a prefix rule, then
 * repeatedly consumes infix operators as long as they are strong enough to take
 * the current expression as their left operand.
 */
static void parsePrecedence(Precedence precedence) {
    advance();

    // Handles prefix operators and literals
    ParseFn prefixRule = getRule(parser.previous.type)->prefix;
    if (prefixRule == NULL) {
        error("Expect expression.");
        return;
    }

    prefixRule();
    while (precedence <= getRule(parser.current.type)->precedence) {
        advance();
        ParseFn infixRule = getRule(parser.previous.type)->infix;
        infixRule();
    }
}

static ParseRule* getRule(TokenType type) { return &rules[type]; }

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

static void expression() { parsePrecedence(PREC_ASSIGNMENT); }

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