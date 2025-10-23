/*
 * Module to represent a chunk of bytecode.
 */

#ifndef clox_chunk_h
#define clox_chunk_h

#include "common.h"
#include "value.h"

/*
 * Defines what kind of instruction we are dealing with.
 */
typedef enum {
    OP_CONSTANT,  // Produce a particular constant
    OP_NIL,
    OP_TRUE,
    OP_FALSE,
    OP_NEGATE,  // -x
    OP_ADD,
    OP_SUBTRACT,
    OP_MULTIPLY,
    OP_DIVIDE,
    OP_RETURN,  // Return from the current function
} OpCode;

/*
 * Defines a series of instructions.
 */
typedef struct {
    int count;
    int capacity;
    uint8_t* code;
    int* lines;  // Store the respective line numbers
    ValueArray constants;
} Chunk;

// Initialize a new chunk
void initChunk(Chunk* chunk);

// Free a chunk of memory
void freeChunk(Chunk* chunk);

// Append a byte to the end of the chunk
void writeChunk(Chunk* chunk, uint8_t byte, int line);

// Add a constant to the chunk's valuearray
int addConstant(Chunk* chunk, Value value);

#endif