SPULIB_TOP					:= $(shell cd ../.. && pwd)
export SPULIB_SRC_DIR		:= $(SPULIB_TOP)/src
SPULIB_BUILD_DIR			:= $(SPULIB_TOP)/build
export SPULIB_OUTPUT_DIR	:= $(SPULIB_TOP)/lib

MAKE_PPU	:= $(MAKE) -C $(SPULIB_BUILD_DIR)/ppu \
                       -f $(SPULIB_SRC_DIR)/ppu/make.dir

.PHONY: all clean

all:
	@mkdir -p $(SPULIB_BUILD_DIR)/ppu
	@$(MAKE_PPU)

clean:
	@$(MAKE_PPU) clean
	@rmdir -p --ignore-fail-on-non-empty $(SPULIB_BUILD_DIR)/ppu
