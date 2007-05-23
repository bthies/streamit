SPULIB_TOP					:= $(shell cd ../.. && pwd)
export SPULIB_SRC_DIR		:= $(SPULIB_TOP)/src
export SPULIB_OUTPUT_DIR	:= $(SPULIB_TOP)/lib

MAKE_PPU	:= $(MAKE) -f $(SPULIB_SRC_DIR)/ppu/make.dir

.PHONY: all clean

all:
	@$(MAKE_PPU)

clean:
	@$(MAKE_PPU) clean
