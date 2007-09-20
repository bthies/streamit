PROGRAM			:= fft
PPU_SOURCES		:= fft.c misc.c ds.c dsrun.c dssched.c
PPU_IMPORTS		:= -lm
SPU_PROGRAMS	:= fft_spu
fft_spu_SOURCES	:= work.c

include $(SPULIB_TOP)/make.inc

.PHONY: diff cleanall

diff:
	@diff fft.out ../output/FFT5.out

cleanall: clean
	@rm -f fft.out
