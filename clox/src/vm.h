#ifndef clox_vm_h
#define clox_vm_h

#include "chunk.h"
#include "value.h"
#define STACK_MAX 256

typedef struct {
    Chunk* chunk;
    uint8_t* ip;  // Instruction pointer, points to the next instruction
    Value stack[STACK_MAX];
    Value* stackTop;  // Points to past the last element
} VM;

typedef enum {
    INTERPRET_OK,
    INTERPRET_COMPILE_ERROR,
    INTERRET_RUNTIME_ERROR
} InterpretResult;

void initVM();
void freeVM();
InterpretResult interpret(Chunk* chunk);

// Stack operations
void push(Value value);
Value pop();

#endif