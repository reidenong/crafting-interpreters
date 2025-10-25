#include "object.h"

#include <stdio.h>
#include <string.h>

#include "memory.h"
#include "object.h"
#include "value.h"
#include "vm.h"

/*
 * Heap allocation for clox objects
 *
 * ALLOCATE_OBJ is a macro to simplify heap allocation
 *
 * allocateObject() calls clox's allocator reallocate() and performs something
 * similar to a malloc(size) and then returns a pointer pointing to this area.
 */
#define ALLOCATE_OBJ(type, objectType) \
    (type*)allocateObject(sizeof(type), objectType)

static Obj* allocateObject(size_t size, ObjType type) {
    Obj* object = (Obj*)reallocate(NULL, 0, size);  // malloc(size)
    object->type = type;                            // records the type

    // Extend the global object list from the head -- the vm.objects list always
    // points to the most recently created object.
    object->next = vm.objects;
    vm.objects = object;
    return object;
}

/*
 * Creates a string object in the heap.
 *
 * Creates a new ObjString in the heap and then initializes its fields.
 */
static ObjString* allocateString(char* chars, int length) {
    ObjString* string = ALLOCATE_OBJ(ObjString, OBJ_STRING);
    string->length = length;
    string->chars = chars;
    return string;
}

/*
 * Claims ownership of the string chars given.
 */
ObjString* takeString(char* chars, int length) {
    return allocateString(chars, length);
}

/*
 * Takes a C string and creates a new clox string object.
 */
ObjString* copyString(const char* chars, int length) {
    char* heapChars = ALLOCATE(char, length + 1);  // create a new C string
    memcpy(heapChars, chars, length);              // copy contents
    heapChars[length] = '\0';                      // terminate
    return allocateString(heapChars, length);      // create the clox string
}

/*
 * Prints a representation of an object based on its type
 */
void printObject(Value value) {
    switch (OBJ_TYPE(value)) {
        case OBJ_STRING:
            printf("%s", AS_CSTRING(value));
            break;
    }
}