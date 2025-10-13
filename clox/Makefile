# Minimal Makefile for clox (put this file in clox/)
CC      ?= cc
CFLAGS  ?= -std=c11 -Wall -Wextra -g

SRC     := $(wildcard src/*.c)
OBJDIR  := build/obj
BINDIR  := build
BIN     := $(BINDIR)/clox
OBJ     := $(patsubst src/%.c,$(OBJDIR)/%.o,$(SRC))

.PHONY: all clean run

all: $(BIN)

$(BIN): $(OBJ)
	@mkdir -p $(BINDIR)
	$(CC) $(OBJ) -o $@

$(OBJDIR)/%.o: src/%.c
	@mkdir -p $(OBJDIR)
	$(CC) $(CFLAGS) -c $< -o $@

run: $(BIN)
	$(BIN) $(ARGS)

clean:
	rm -rf build
