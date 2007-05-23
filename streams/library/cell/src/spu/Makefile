SPULIB_TOP					:= $(shell cd ../.. && pwd)
export SPULIB_SRC_DIR		:= $(SPULIB_TOP)/src
export SPULIB_OUTPUT_DIR	:= $(SPULIB_TOP)/lib

MAKE_SPU	:= $(MAKE) -f $(SPULIB_SRC_DIR)/spu/make.dir

.PHONY: all clean

all:
	@$(MAKE_SPU)

clean:
	@$(MAKE_SPU) clean
