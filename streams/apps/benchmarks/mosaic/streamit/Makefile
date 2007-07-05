default: lib

# library
lib: 1b.bin 2b.bin
	strc -library -nosched mosaic.str FFT.str fundmatrix.str ransac.str correlation.str feature.str

# compiler
comp: 1b.bin 2b.bin
	strc -dynamicRatesEverywhere mosaic.str FFT.str fundmatrix.str ransac.str correlation.str feature.str
# then do: ./a.out -i 460800

# simple version in library
simple-lib:
	cat bin/1.bin > bin/combined-inputs.bin
	cat bin/2.bin >> bin/combined-inputs.bin
	strc -library -i 2 mosaic_simple.str feature.str
	head -c 921600 combined-outputs.bin > 1out.bin
	tail -c 921600 combined-outputs.bin > 2out.bin

# simple version in compiler (uniprocessor)
simple-comp:
	cat bin/1.bin > bin/combined-inputs.bin
	cat bin/2.bin >> bin/combined-inputs.bin
	strc mosaic_simple.str feature.str
	./a.out -i 2
	head -c 921600 combined-outputs.bin > 1out.bin
	tail -c 921600 combined-outputs.bin > 2out.bin

# simple version in compiler (cluster)
simple-cluster:
	cat bin/1.bin > bin/combined-inputs.bin
	cat bin/2.bin >> bin/combined-inputs.bin
	strc -cluster 5 -pgreedier mosaic_simple.str feature.str
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

# 1b.bin contains two copies of the binary input 1.bin.  same for 2b.bin.
# we need this because of peeking -- even if running just 1 iter, have to peek ahead to next
1b.bin: 1.bin
	cat 1.bin > 1b.bin
	cat 1.bin >> 1b.bin

2b.bin: 2.bin
	cat 2.bin > 2b.bin
	cat 2.bin >> 2b.bin

# make the binary inputs from the .ppm files
1.bin: ../input/1.ppm
	${STREAMIT_HOME}/misc/dat2bin.pl --ppm ../input/1.ppm 1.bin


2.bin: ../input/2.ppm
	${STREAMIT_HOME}/misc/dat2bin.pl --ppm ../input/2.ppm 2.bin
