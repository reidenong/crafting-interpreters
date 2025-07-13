BUILD_DIR := build
CLEAN_TARGETS :=

# =========================================================================
# JLOX (Java Implementation)
# =========================================================================
JLOX_SRC_DIR := jlox/lox
JLOX_TOOL_DIR := jlox/tool
JLOX_MAIN_CLASS := jlox.lox.Lox
JLOX_BUILD_ROOT := $(BUILD_DIR)
JLOX_JAR_NAME := $(JLOX_BUILD_ROOT)/jlox.jar
JLOX_MANIFEST_FILE := $(JLOX_BUILD_ROOT)/MANIFEST.MF

.PHONY: jlox jlox-run jlox-clean

CLEAN_TARGETS += $(JLOX_BUILD_ROOT)/jlox $(JLOX_JAR_NAME)

jlox: $(JLOX_JAR_NAME)

$(JLOX_JAR_NAME): $(JLOX_SRC_DIR)/*.java
	@mkdir -p $(JLOX_BUILD_ROOT)

	@javac jlox/tool/GenerateAst.java
	@java jlox.tool.GenerateAst ./jlox/lox/

	@javac -d $(JLOX_BUILD_ROOT) $(JLOX_SRC_DIR)/*.java

	@echo "Main-Class: $(JLOX_MAIN_CLASS)" > $(JLOX_MANIFEST_FILE)
	@echo "" >> $(JLOX_MANIFEST_FILE)

	@jar cvfm $@ $(JLOX_MANIFEST_FILE) -C $(JLOX_BUILD_ROOT) .
	@rm $(JLOX_MANIFEST_FILE)

# Target to run the jlox interpreter
jlox-run: $(JLOX_JAR_NAME)
	@java -jar $(JLOX_JAR_NAME) $(args)

# Target to clean jlox build artifacts
jlox-clean:
	@rm -rf $(JLOX_BUILD_PATH) $(JLOX_JAR_NAME)
	@echo "--- JLOX clean complete ---"

# =========================================================================
# General Makefile targets
# =========================================================================

# Default target (builds all implementations you define in 'all')
all: jlox # Add other implementations here, e.g., all: jlox clox

# General clean target to remove all build artifacts from all implementations
clean:
	@rm -rf $(CLEAN_TARGETS)
	@rm -rf $(BUILD_DIR) # Also remove the top-level build directory if empty or if we want to ensure it's gone
	@echo "--- General clean complete ---"