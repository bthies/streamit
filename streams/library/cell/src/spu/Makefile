SPULIB_TOP					:= $(shell cd ../.. && pwd)
export SPULIB_SRC_DIR		:= $(SPULIB_TOP)/src
SPULIB_BUILD_DIR			:= $(SPULIB_TOP)/build
export SPULIB_OUTPUT_DIR	:= $(SPULIB_TOP)/lib

MAKE_SPU	:= $(MAKE) -C $(SPULIB_BUILD_DIR)/spu \
                       -f $(SPULIB_SRC_DIR)/spu/make.dir

.PHONY: all clean

all:
	@mkdir -p $(SPULIB_BUILD_DIR)/spu
	@$(MAKE_SPU)

clean:
	@$(MAKE_SPU) clean
	@rmdir -p --ignore-fail-on-non-empty $(SPULIB_BUILD_DIR)/spu
