#include "memory.h"

#include <stdlib.h>

/*
 * Handles (all) dynamic memory allocation for clox
 */
void* reallocate(void* pointer, size_t oldSize, size_t newSize) {
    // Handle deallocation by ourselves
    if (newSize == 0) {
        free(pointer);
        return NULL;
    }

    // When oldSize is 0, realloc == malloc
    // We shrink or grow the allocation if there is capacity
    // Otherwise, allocate new space and return the pointer
    void* result = realloc(pointer, newSize);

    if (result == NULL) exit(1);  // In case not enough space
    return result;
}