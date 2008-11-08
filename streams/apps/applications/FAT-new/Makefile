all:
	cp ${STREAMIT_HOME}/apps/benchmarks/fft/streamit/FFT2.str .
	# remove the toplevel stream from FFT2
	perl -pi -e 's/void->void/\/* void->void/g' FFT2.str
	perl -pi -e 's/float->float\ filter\ CombineDFT/\*\/float->float\ filter\ CombineDFT/g' FFT2.str
	strc -library -norun -printreps FAT_test.str AdapBeam.str BandProc.str Block45.str CFAR.str CFAR_Vect.str Comp_Rep.str DegenTetr.str DopFilt.str ElPipe.str FAT.str FFT2.str FormChg20.str FormChg21Real.str FormChg21.str GenCub.str GenTetr.str GetCube.str getCub.str PulsCmp.str STAP.str SubAnalysis.str SubSynth.str SunSynth.str Target.str WideBand.str

clean:
	rm -f *.cpp *.c *.java *~ FFT2.str *.dot
