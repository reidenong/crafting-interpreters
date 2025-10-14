#include "vm.h"

#include <stdio.h>

#include "common.h"
#include "debug.h"

VM vm;

/*
 * Helper functions to manage the vm's value stack
 */
static void resetStack() { vm.stackTop = vm.stack; }

void push(Value value) {
    *vm.stackTop = value;
    vm.stackTop++;
}

Value pop() {
    vm.stackTop--;
    return *vm.stackTop;
}

/*
 * Functions to manage the vm
 */
void initVM() { resetStack(); };

void freeVM() {};

/*
 * Runs the virtual machine
 */
static InterpretResult run() {
// Reads the next byte from the instruction stream and advances the instruction
// pointer. ip always points to the next instruction to be run
#define READ_BYTE() (*vm.ip++)

// Reads an index from bytecode and looks up the constant table
#define READ_CONSTANT() (vm.chunk->constants.values[READ_BYTE()])

// Carries out a binary operation
// operators in C are not first class, but here the preprocessor is
// doing text substitutions so we can use op as such.
#define BINARY_OP(op)     \
    do {                  \
        double b = pop(); \
        double a = pop(); \
        push(a op b);     \
    } while (false);

    for (;;) {
#ifdef DEBUG_TRACE_EXECUTION
        /*
         * Stack tracing for debugging, print each value in the stack
         */
        printf("        ");
        for (Value* slot = vm.stack; slot < vm.stackTop; slot++) {
            printf("[ ");
            printValue(*slot);
            printf(" ]");
        }
        printf("\n");
        disassembleInstruction(vm.chunk, (int)(vm.ip - vm.chunk->code));
#endif

        /*
         * Run each bytecode instruction
         */
        uint8_t instruction;
        switch (instruction = READ_BYTE()) {
            case OP_CONSTANT: {
                Value constant = READ_CONSTANT();
                push(constant);
                break;
            }
            case OP_NEGATE:
                push(-pop());
                break;
            case OP_ADD:
                BINARY_OP(+);
                break;
            case OP_SUBTRACT:
                BINARY_OP(-);
                break;
            case OP_MULTIPLY:
                BINARY_OP(*);
                break;
            case OP_DIVIDE:
                BINARY_OP(/);
                break;
            case OP_RETURN: {
                printValue(pop());
                printf("\n");
                return INTERPRET_OK;
            }
        }
    }

#undef READ_BYTE
#undef READ_CONSTANT
#undef BINARY_OP
}

InterpretResult interpret(Chunk* chunk) {
    vm.chunk = chunk;
    vm.ip = vm.chunk->code;  // Instruction-pointer
    return run();            // Run the bytecode
}
