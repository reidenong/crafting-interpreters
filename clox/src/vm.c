#include "vm.h"

#include <stdio.h>

#include "common.h"
#include "debug.h"

VM vm;

void initVM() {};

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

    for (;;) {
#ifdef DEBUG_TRACE_EXECUTION
        disassembleInstruction(vm.chunk, (int)(vm.ip - vm.chunk->code));
#endif

        uint8_t instruction;
        switch (instruction = READ_BYTE()) {
            case OP_CONSTANT: {
                Value constant = READ_CONSTANT();
                printValue(constant);
                printf("\n");
                break;
            }
            case OP_RETURN: {
                return INTERPRET_OK;
            }
        }
    }

#undef READ_BYTE
#undef READ_CONSTANT
}

InterpretResult interpret(Chunk* chunk) {
    vm.chunk = chunk;
    vm.ip = vm.chunk->code;  // Instruction-pointer
    return run();            // Run the bytecode
}
