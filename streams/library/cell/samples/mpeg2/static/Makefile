PROGRAM				:= mpeg
SPU_PROGRAMS		:= mpeg_spu
mpeg_spu_SOURCES	:= filter0.c filter1.c filter2.c filter3.c

include $(SPULIB_TOP)/make.inc

.PHONY: check nocheck
check nocheck:
	@cp -f spulib_config.h.$@ spulib_config.h
	@$(MAKE) clean
	@$(MAKE)
