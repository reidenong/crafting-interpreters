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
typedef void (*ParseFn)(
    bool canAssign);  // Function pointer for parsing functions

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

static void error(const char* message) { errorAt(&parser.previous, message); }

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

static bool check(TokenType type) { return parser.current.type == type; }

/*
 * If the current token has the given type, consume it and return true.
 * Else leave token alone, and return false.
 */
static bool match(TokenType type) {
    if (!check(type)) return false;
    advance();
    return true;
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

#ifdef DEBUG_PRINT_CODEg
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
static void statement();
static void declaration();
static ParseRule* getRule(TokenType type);
static void parsePrecedence(Precedence precedence);

/*
 * Compiles a binary expression.
 */
static void binary(bool canAssign) {
    // Based on the operatorType, we determine which precedence we should
    // continue at
    TokenType operatorType = parser.previous.type;
    ParseRule* rule = getRule(operatorType);
    parsePrecedence((Precedence)(rule->precedence + 1));

    switch (operatorType) {
        case TOKEN_BANG_EQUAL:
            emitBytes(OP_EQUAL, OP_NOT);
            break;
        case TOKEN_EQUAL_EQUAL:
            emitByte(OP_EQUAL);
            break;
        case TOKEN_GREATER:
            emitByte(OP_GREATER);
            break;
        case TOKEN_GREATER_EQUAL:
            emitBytes(OP_LESS, OP_NOT);
            break;
        case TOKEN_LESS:
            emitByte(OP_LESS);
            break;
        case TOKEN_LESS_EQUAL:
            emitBytes(OP_GREATER, OP_NOT);
            break;
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

static void literal(bool canAssign) {
    switch (parser.previous.type) {
        case TOKEN_FALSE:
            emitByte(OP_FALSE);
            break;
        case TOKEN_NIL:
            emitByte(OP_NIL);
            break;
        case TOKEN_TRUE:
            emitByte(OP_TRUE);
            break;
        default:
            return;  // Unreachable.
    }
}

/*
 * Compiles an unary expression.
 */
static void unary(bool canAssign) {
    TokenType operatorType = parser.previous.type;

    // Compile the operand.
    parsePrecedence(PREC_UNARY);
    // we parse at the same level of precedence to allow things like !!x

    // Emit the operator instruction.
    switch (operatorType) {
        case TOKEN_BANG:
            emitByte(OP_NOT);
            break;
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

    /*
     * If we are doing assignment, this is at the lowest level of precedence, so
     * we can only do it if we are doing other things at this same level of
     * precedence (eg. nested assignment)
     */
    bool canAssign = precedence <= PREC_ASSIGNMENT;
    prefixRule(canAssign);

    while (precedence <= getRule(parser.current.type)->precedence) {
        advance();
        ParseFn infixRule = getRule(parser.previous.type)->infix;
        infixRule(canAssign);
    }

    /*
     * If the assignment token hasn't been consumed, its been ignored because
     * somewhere the syntax is wrong. we return an error here.
     */
    if (canAssign && match(TOKEN_EQUAL)) {
        error("Invalid assignment target.");
    }
}

/*
 * Adds a constant to the vm's constant table.
 *
 * Identifier string is to large to be stored in the vm, so we add it to the
 * vm's constant table and access it using its index.
 */
static uint8_t identifierConstant(Token* name) {
    return makeConstant(OBJ_VAL(copyString(name->start, name->length)));
}

static uint8_t parseVariable(const char* errorMessage) {
    consume(TOKEN_IDENTIFIER, errorMessage);
    return identifierConstant(&parser.previous);
}

static void defineVariable(uint8_t global) {
    emitBytes(OP_DEFINE_GLOBAL, global);
}

/*
 * Compiles a grouping expression.
 */
static void grouping(bool canAssign) {
    expression();
    consume(TOKEN_RIGHT_PAREN, "Expect ')' after expression.");
    // assume open bracket has been consumed
}

/*
 * Compiles a number literal.
 */
static void number(bool canAssign) {
    double value = strtod(parser.previous.start, NULL);
    // strtod: string to double, stops automatically when it reaches the first
    // non-numeric character
    emitConstant(NUMBER_VAL(value));
}

/*
 * Compiles a string literal.
 */
static void string(bool canAssign) {
    emitConstant(OBJ_VAL(
        copyString(parser.previous.start + 1, parser.previous.length - 2)));
}

/*
 * Compiles a variable access.
 */
static void namedVariable(Token name, bool canAssign) {
    uint8_t arg = identifierConstant(&name);

    if (canAssign && match(TOKEN_EQUAL)) {
        expression();
        emitBytes(OP_SET_GLOBAL, arg);  // Variable assignment
    } else {
        emitBytes(OP_GET_GLOBAL, arg);  // Variable access
    }
}
static void variable(bool canAssign) {
    namedVariable(parser.previous, canAssign);
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
    [TOKEN_BANG] = {unary, NULL, PREC_NONE},
    [TOKEN_BANG_EQUAL] = {NULL, binary, PREC_EQUALITY},
    [TOKEN_EQUAL] = {NULL, NULL, PREC_NONE},
    [TOKEN_EQUAL_EQUAL] = {NULL, binary, PREC_EQUALITY},
    [TOKEN_GREATER] = {NULL, binary, PREC_COMPARISON},
    [TOKEN_GREATER_EQUAL] = {NULL, binary, PREC_COMPARISON},
    [TOKEN_LESS] = {NULL, binary, PREC_COMPARISON},
    [TOKEN_LESS_EQUAL] = {NULL, binary, PREC_COMPARISON},
    [TOKEN_IDENTIFIER] = {variable, NULL, PREC_NONE},
    [TOKEN_STRING] = {string, NULL, PREC_NONE},
    [TOKEN_NUMBER] = {number, NULL, PREC_NONE},
    [TOKEN_AND] = {NULL, NULL, PREC_NONE},
    [TOKEN_CLASS] = {NULL, NULL, PREC_NONE},
    [TOKEN_ELSE] = {NULL, NULL, PREC_NONE},
    [TOKEN_FALSE] = {literal, NULL, PREC_NONE},
    [TOKEN_FOR] = {NULL, NULL, PREC_NONE},
    [TOKEN_FUN] = {NULL, NULL, PREC_NONE},
    [TOKEN_IF] = {NULL, NULL, PREC_NONE},
    [TOKEN_NIL] = {literal, NULL, PREC_NONE},
    [TOKEN_OR] = {NULL, NULL, PREC_NONE},
    [TOKEN_PRINT] = {NULL, NULL, PREC_NONE},
    [TOKEN_RETURN] = {NULL, NULL, PREC_NONE},
    [TOKEN_SUPER] = {NULL, NULL, PREC_NONE},
    [TOKEN_THIS] = {NULL, NULL, PREC_NONE},
    [TOKEN_TRUE] = {literal, NULL, PREC_NONE},
    [TOKEN_VAR] = {NULL, NULL, PREC_NONE},
    [TOKEN_WHILE] = {NULL, NULL, PREC_NONE},
    [TOKEN_ERROR] = {NULL, NULL, PREC_NONE},
    [TOKEN_EOF] = {NULL, NULL, PREC_NONE},
};

static ParseRule* getRule(TokenType type) { return &rules[type]; }

static void expression() { parsePrecedence(PREC_ASSIGNMENT); }

static void varDeclaration() {
    uint8_t global =
        parseVariable("Expect variable name.");  // index of the constnat in the
                                                 // vm's constant table.

    if (match(TOKEN_EQUAL)) {
        expression();  // Initializes variable with expression.
    } else {
        emitByte(OP_NIL);  // var a;
    }

    consume(TOKEN_SEMICOLON, "Expect ';' after variable declaration.");
    defineVariable(global);  // Access constant table by its index.
}

/*
 * A expression followed by a semicolon
 *
 * How you write a expression where a statement is expected, usually to evaluate
 * something for its side effects. eg.: eat(brunch);
 */
static void expressionStatement() {
    expression();
    consume(TOKEN_SEMICOLON, "Expect ';' after expression.");
    emitByte(OP_POP);
}

static void printStatement() {
    expression();
    consume(TOKEN_SEMICOLON, "Expect ';' after value.");
    emitByte(OP_PRINT);
}

/*
 * Activated when the compiler detects errors, acts as a synchronization point
 * to eliminate the number of cascading errors.
 *
 * Skip statements until we reach something that looks like a statement
 * boundary, ie. a semicolon. then continue compiling.
 */
static void synchronize() {
    parser.panicMode = false;

    while (parser.current.type != TOKEN_EOF) {
        if (parser.previous.type == TOKEN_SEMICOLON) return;
        switch (parser.current.type) {
            case TOKEN_CLASS:
            case TOKEN_FUN:
            case TOKEN_VAR:
            case TOKEN_FOR:
            case TOKEN_IF:
            case TOKEN_WHILE:
            case TOKEN_PRINT:
            case TOKEN_RETURN:
                return;
            default:;  // Do nothing
        }
        advance();
    }
}

/*
 * Compiles a declaration statement.
 */
static void declaration() {
    if (match(TOKEN_VAR)) {
        varDeclaration();
    } else {
        statement();
    }

    if (parser.panicMode)
        synchronize();  // error recovery to minimize the number of cascading
                        // compile errors, use each statement as the boundary to
                        // synchronize errors.
}

static void statement() {
    if (match(TOKEN_PRINT)) {
        printStatement();
    } else {
        expressionStatement();
    }
}

bool compile(const char* source, Chunk* chunk) {
    initScanner(source);
    compilingChunk = chunk;

    parser.hadError = false;
    parser.panicMode = true;

    advance();

    while (!match(TOKEN_EOF)) {
        declaration();
    }

    endCompiler();  // Finish compiling code; send OP_RETURN
    return !parser.hadError;
}