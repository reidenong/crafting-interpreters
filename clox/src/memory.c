#include "memory.h"

#include <stdlib.h>

#include "vm.h"

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

/*
 * Free the global object list for the vm.
 *
 * Traverse the global object list, free it, then move on to the next one.
 */
static void freeObject(Obj* object) {
    switch (object->type) {
        case OBJ_STRING: {
            ObjString* string = (ObjString*)object;
            FREE_ARRAY(char, string->chars, string->length + 1);
            FREE(ObjString, object);
            break;
        }
    }
}

void freeObjects() {
    Obj* object = vm.objects;
    while (object != NULL) {
        Obj* next = object->next;
        freeObject(object);
        object = next;
    }
}