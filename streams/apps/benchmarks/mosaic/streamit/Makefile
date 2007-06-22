default: lib

# library
lib:
	strc -library -nosched mosaic.str FFT.str fundmatrix.str ransac.str correlation.str feature.str

# compiler
comp:
	strc -dynamicRatesEverywhere mosaic.str FFT.str fundmatrix.str ransac.str correlation.str feature.str

# simple version in library
simple-lib:
	cat bin/1.bin > bin/combined-inputs.bin
	cat bin/2.bin >> bin/combined-inputs.bin
	strc -library -i 2 mosaic_simple.str feature.str
	head -c 921600 combined-outputs.bin > 1out.bin
	tail -c 921600 combined-outputs.bin > 2out.bin

# simple version in compiler
simple-comp:
	cat bin/1.bin > bin/combined-inputs.bin
	cat bin/2.bin >> bin/combined-inputs.bin
	strc mosaic_simple.str feature.str
	./a.out -i 2
	head -c 921600 combined-outputs.bin > 1out.bin
	tail -c 921600 combined-outputs.bin > 2out.bin

# run both library and compiler and compare results
simple-verify:
# run library
	$(MAKE) simple-lib
	${STREAMIT_HOME}/misc/bin2dat.pl 1out.bin > 1out.library.txt
	${STREAMIT_HOME}/misc/bin2dat.pl 2out.bin > 2out.library.txt
# run compiler
	$(MAKE) simple-comp
	${STREAMIT_HOME}/misc/bin2dat.pl 1out.bin > 1out.compiler.txt
	${STREAMIT_HOME}/misc/bin2dat.pl 2out.bin > 2out.compiler.txt
# compare results
	${STREAMIT_HOME}/regtest/tools/compare_abs.pl 1out.compiler.txt 1out.library.txt
	${STREAMIT_HOME}/regtest/tools/compare_abs.pl 2out.compiler.txt 2out.library.txt
