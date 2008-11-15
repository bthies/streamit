all:
	cp ${STREAMIT_HOME}/apps/benchmarks/fft/streamit/FFT2.str .
	# remove the toplevel stream from FFT2
	perl -pi -e 's/void->void/\/* void->void/g' FFT2.str
	perl -pi -e 's/float->float\ filter\ CombineDFT/\*\/float->float\ filter\ CombineDFT/g' FFT2.str
	strc -library -i 10 MPDChain.str CFARDetect.str CFARLMax.str CFARNoiseLevel.str CFAR.str CFARToDecibel.str CFARTreshold.str CornerTurn.str DopplerFilter.str FFT2.str IFFTKernel.str FIR.str PulseCompressGuard.str PulseCompression.str

clean:
	rm -rf *.class *.java FFT2.str *.dot *.pdf .ps *~

