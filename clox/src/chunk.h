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
    OP_RETURN,  // Return from the current function
} OpCode;

/*
 * Defines a series of instructi ons.
 */
typedef struct {
    int count;
    int capacity;
    uint8_t* code;
    ValueArray constants;
} Chunk;

// Initialize a new chunk
void initChunk(Chunk* chunk);

// Free a chunk of memory
void freeChunk(Chunk* chunk);

// Append a byte to the end of the chunk
void writeChunk(Chunk* chunk, uint8_t byte);

// Add a constant to the chunk's valuearray
void addConstant(Chunk* chunk, Value value) {
    writeValueArray(&chunk->constants, value);
    return chunk->constants.count - 1;  // return its index for later lookup
}

#endif