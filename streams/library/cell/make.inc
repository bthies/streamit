# The following variables must be defined in the program makefile (optional
# ones are indicated by *):
#
# PROGRAM := filename of PPU program executable
# SPU_PROGRAMS := list of names for each distinct SPU program
#
# *<spu_program>_SOURCES := list of .c source files for this SPU program. If
#   undefined, assumes the only file is <spu_program>.c.
# *<spu_program>_IMPORTS := additional libraries to link this SPU program with
#
# *SPU_COMMON_SOURCES := list of .c source files common to all SPU programs
#   (repetitions are okay)
# *SPU_INCLUDE := additional include paths for all SPU programs
# *SPU_COMMON_IMPORTS := additional libraries to link all SPU programs with
#
# *PPU_SOURCES := list of .c source files for PPU program. If undefined,
#   assumes the only file is <program>.c.
# *PPU_INCLUDE := additional include paths for PPU program
# *PPU_IMPORTS := additional libraries to link PPU program with
#
# *COMMON_SOURCES := list of .c source files common to SPU and PPU programs
#
# *SPU_LAYOUT := list of SPU program names. Entry i is the program loaded onto
#   SPU i. If undefined and there is only one SPU program, it is loaded onto
#   all SPUs, otherwise SPU_PROGRAMS must specify 6 SPU programs and is used as
#   the order.
#
# *OPT_LEVEL := optimization level (defaults to -g)
#
# Include this file afterwards.

export SRC_DIR		:= $(shell pwd)
BUILD_DIR			:= $(SRC_DIR)/build

# Rebuild library if there's a custom config header or optimization level
SPULIB_CONFIG_FILE	:= $(SRC_DIR)/spulib_config.h

BUILD_SPULIB		:= 0

ifneq "$(wildcard $(SPULIB_CONFIG_FILE))" ""
  BUILD_SPULIB		:= 1
  export SPULIB_CONFIG_FILE
endif

ifdef OPT_LEVEL
  CC_OPT_LEVEL		:= $(OPT_LEVEL)
  SPULIB_OPT_LEVEL	:= $(OPT_LEVEL)
else
  CC_OPT_LEVEL		:= -g
endif

export CC_OPT_LEVEL

ifdef SPULIB_OPT_LEVEL
  BUILD_SPULIB		:= 1
  export SPULIB_OPT_LEVEL
endif

ifeq "$(BUILD_SPULIB)" "1"
  export SPULIB_SRC_DIR		:= $(SPULIB_TOP)/src
  export SPULIB_BUILD_DIR	:= $(BUILD_DIR)/spulib
  SPULIB_OUTPUT_DIR			:= $(SPULIB_BUILD_DIR)
else
  SPULIB_OUTPUT_DIR			:= $(SPULIB_TOP)/lib
endif

export SPULIB_OUTPUT_DIR

# Filename sorting

SPU_COMMON_SOURCES	:= $(SPU_COMMON_SOURCES) $(COMMON_SOURCES)
spu_sources			 = $(sort $(if $($(1)_SOURCES),$($(1)_SOURCES),$(1).c) \
                              $(SPU_COMMON_SOURCES))
spu_objs			 = $(patsubst %.c,%.o,$(call spu_sources,$(1)))
spu_imports			 = $(sort $($(1)_IMPORTS) $(SPU_COMMON_IMPORTS))
PPU_SOURCES			:= $(sort $(if $(PPU_SOURCES),$(PPU_SOURCES), \
                                                  $(PROGRAM).c) \
                              $(COMMON_SOURCES))
PPU_OBJS			:= $(PPU_SOURCES:.c=.o)

ifdef SPU_INCLUDE
  export SPU_INCLUDE
endif

ifdef PPU_INCLUDE
  export PPU_INCLUDE
endif

# Default "layout".
ifndef SPU_LAYOUT
  SPU_PROGRAMS		:= $(strip $(SPU_PROGRAMS))
ifeq "$(firstword $(SPU_PROGRAMS))" "$(SPU_PROGRAMS)"
  SPU_LAYOUT		:= $(SPU_PROGRAMS) $(SPU_PROGRAMS) $(SPU_PROGRAMS) \
                       $(SPU_PROGRAMS) $(SPU_PROGRAMS) $(SPU_PROGRAMS)
else
  SPU_LAYOUT		:= $(SPU_PROGRAMS)
endif
endif

.PHONY: all clean $(SPU_PROGRAMS) spulib builddir

# SPU_INCLUDE, PPU_INCLUDE, CC_OPT_LEVEL are exported to SDK makefiles
#
# SPU makefile needs to have exported:
# - SPU_PROGRAM - name
# - OBJS - list of objects
# - *IMPORTS - additional libraries
#
# PPU makefile needs to have exported PPU_PROGRAM, OBJS, *IMPORTS and also
# value of SPU_PROGRAMS

all: builddir spulib $(SPU_PROGRAMS)
	@cd $(BUILD_DIR) && $(SPULIB_TOP)/spusymbols.pl $(SPU_PROGRAMS)
	@cd $(BUILD_DIR) && $(SPULIB_TOP)/spuinit.pl $(SPU_LAYOUT)
	@PPU_PROGRAM=$(PROGRAM) \
	 OBJS='$(PPU_OBJS)' \
	 SPU_PROGRAMS='$(SPU_PROGRAMS)' \
	 IMPORTS='$(PPU_IMPORTS)' \
	 $(MAKE) -C $(BUILD_DIR) -f $(SPULIB_TOP)/make.ppu
ifneq "$(SRC_DIR)" "$(BUILD_DIR)"
	@cp -upf $(BUILD_DIR)/$(PROGRAM) $(SRC_DIR)/$(PROGRAM)
endif

$(SPU_PROGRAMS): builddir spulib
	@SPU_PROGRAM=$@ \
	 OBJS='$(call spu_objs,$@)' \
	 IMPORTS='$(call spu_imports,$@)' \
	 $(MAKE) -C $(BUILD_DIR) -f $(SPULIB_TOP)/make.spu

spulib: builddir
ifeq "$(BUILD_SPULIB)" "1"
	@$(MAKE) -f $(SPULIB_SRC_DIR)/make.dir
endif

builddir:
	@mkdir -p $(BUILD_DIR)

clean:
# TODO: this is probably very unsafe
	@rm -rf $(BUILD_DIR)
	@rm -f $(SRC_DIR)/$(PROGRAM)
