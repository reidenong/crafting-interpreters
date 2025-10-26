#include "table.h"

#include <stdlib.h>
#include <string.h>

#include "memory.h"
#include "object.h"
#include "value.h"

#define TABLE_MAX_LOAD 0.75

void initTable(Table* table) {
    table->count = 0;
    table->capacity = 0;
    table->entries = NULL;
}

void freeTable(Table* table) {
    FREE_ARRAY(Entry, table->entries, table->capacity);
    initTable(table);
}

/*
 * Implements hash lookup with linear probing.
 *
 * Locates the slot in a hash table that corresponds to a given key: either the
 * slot that is already holding it or the slot where it should be inserted
 *
 * Efficiently takes care of tombstones by returning either the first truly
 * empty place or the first tombstone location if the key is not already inside.
 */
static Entry* findEntry(Entry* entries, int capacity, ObjString* key) {
    uint32_t index = key->hash % capacity;
    Entry* tombstone = NULL;

    for (;;) {
        Entry* entry = &entries[index];
        if (entry->key == NULL) {
            if (IS_NIL(entry->value)) {  // Really empty
                return tombstone != NULL ? tombstone : entry;
            } else {  // tombstone
                if (tombstone == NULL) tombstone = entry;
            }
        } else if (entry->key == key) {  // found the key
            return entry;
        }

        index = (index + 1) % capacity;
    }
}

/*
 * Retrieves a value from a table if it is present.
 *
 * Return value is its presence in table.
 * If present, its value is loaded in the input value pointer.
 */
bool tableGet(Table* table, ObjString* key, Value* value) {
    if (table->count == 0) return false;  // Don't access bucket array if NULL

    Entry* entry = findEntry(table->entries, table->capacity, key);
    if (entry->key == NULL) return false;

    *value = entry->value;
    return true;
}

/*
 * Allocates an array of buckets.
 */
static void adjustCapacity(Table* table, int capacity) {
    Entry* entries = ALLOCATE(Entry, capacity);
    for (int i = 0; i < capacity; i++) {
        entries[i].key = NULL;
        entries[i].value = NIL_VAL;
    }

    // If we are growing a existing table, we need to reinsert all the old
    // elements. Do a linear pass through the old table, reinsert all of the
    // previous elements first
    table->capacity = 0;
    for (int i = 0; i < table->capacity; i++) {
        Entry* entry = &table->entries[i];
        if (entry->key == NULL) continue;

        Entry* dest = findEntry(entries, capacity, entry->key);
        dest->key = entry->key;
        dest->value = entry->value;
        table->count++;
    }

    FREE_ARRAY(Entry, table->entries, table->capacity);
    table->entries = entries;
    table->capacity = capacity;
}

/*
 * Adds a key/pair value to a given hash table.
 *
 * If entry is already present, overwrite with new value.
 * Else create new hash table entry, update count for load factor.
 */
bool tableSet(Table* table, ObjString* key, Value value) {
    // Grow table if we exceed ideal load factor
    if (table->count + 1 > table->capacity * TABLE_MAX_LOAD) {
        int capacity = GROW_CAPACITY(table->capacity);
        adjustCapacity(table, capacity);
    }

    Entry* entry = findEntry(table->entries, table->capacity, key);
    bool isNewKey = entry->key == NULL;
    if (isNewKey && IS_NIL(entry->value))
        table->count++;  // increment count if we are inserting into truly new
                         // bucket, and not existing tombstone

    entry->key = key;
    entry->value = value;
    return isNewKey;
}

/*
 * Implements deletion in a table with tombstones.
 */
bool tableDelete(Table* table, ObjString* key) {
    if (table->count == 0) return false;

    // Find the entry
    Entry* entry = findEntry(table->entries, table->capacity, key);
    if (entry->key == NULL) return false;

    // Place a tombstone
    entry->key = NULL;
    entry->value = BOOL_VAL(true);
    return true;
}

void tableAddAll(Table* from, Table* to) {
    for (int i = 0; i < from->capacity; i++) {
        Entry* entry = &from->entries[i];
        if (entry->key != NULL) {
            tableSet(to, entry->key, entry->value);
        }
    }
}

/*
 * Look for a string in a hash table.
 */
ObjString* tableFindString(Table* table, const char* chars, int length,
                           uint32_t hash) {
    if (table->count == 0) return NULL;

    uint32_t index = hash % table->capacity;
    for (;;) {
        Entry* entry = &table->entries[index];
        if (entry->key == NULL) {  // Stop if we find a truly empty spot.
            if (IS_NIL(entry->value)) return NULL;
        } else if (entry->key->length == length && entry->key->hash == hash &&
                   memcmp(entry->key->chars, chars, length) == 0) {
            // Only place in the vm where we check for string equality, after
            // interning as long as the strings are in two different locations
            // they are different strings.
            return entry->key;
        }

        index = (index + 1) % table->capacity;
    }
}