#ifndef clox_value_h
#define clox_value_h

#include "common.h"

typedef double Value;

// Constant Pool, an (dynamic) array of values
typedef struct {
    int capacity;
    int count;
    Value* values;
} ValueArray;

void initialValueArray(ValueArray* array);
void writeValueArray(ValueArray* array, Value value);
void freeValueArray(ValueArray* array);

#endif