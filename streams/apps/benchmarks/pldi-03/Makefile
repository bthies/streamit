FRONTEND = java streamit.frontend.ToJava
COMPILER = java -Xmx1500M  at.dms.kjc.Main -s
SL       = gcc -O2 -lm -I/u/aalamb/streams/library/c /u/aalamb/streams/library/c/stream*.c

all: clean benchmarks



fir-results: FIRProgram.java
	./reap_freq.pl scripts/fir.script
target-results: TargetDetect.java
	./reap_freq.pl scripts/target.script
filterbank-results: FilterBank.java
	./reap_freq.pl scripts/filterbank.script
sampling-results: SamplingRateConverter.java
	./reap_freq.pl scripts/sample.script
test-results: Test.java
	./reap_freq.pl scripts/test.script

fm-results: FMRadioApp.java
	./reap_freq.pl scripts/fmradio.script
beamformer-results: CoarseSerializedBeamFormer.java
	./reap_freq.pl scripts/beamformer.script



benchmarks: FIRProgram.java SamplingRateConverter.java FilterBank.java TargetDetect.java Test.java


# rules to make a directory and run all of the benchmarks so that I can measure some metrics
dump: benchmarks
	mkdir -p dump
	$(COMPILER) --unroll 10000000 --linearreplacement --frequencyreplacement 1 FIRProgram.java
	cp linear.dot dump/fir-linear.dot
	cp linear-replace.dot dump/fir-linear-replace.dot
	cp linear-frequency.dot dump/fir-freq.dot
	$(COMPILER)  --unroll 10000000 --frequencyreplacement 1 FIRProgram.java
	cp linear-frequency.dot dump/fir-freq-only.dot

	$(COMPILER)  --unroll 10000000 --linearreplacement --frequencyreplacement 1 SamplingRateConverter.java
	cp linear.dot dump/sample-linear.dot
	cp linear-replace.dot dump/sample-linear-replace.dot
	cp linear-frequency.dot dump/sample-freq.dot
	$(COMPILER)  --unroll 10000000 --frequencyreplacement 1 SamplingRateConverter.java
	cp linear-frequency.dot dump/sample-freq-only.dot


	$(COMPILER)  --unroll 10000000 --linearreplacement --frequencyreplacement 1 FilterBank.java
	cp linear.dot dump/filterbank-linear.dot
	cp linear-replace.dot dump/filterbank-linear-replace.dot
	cp linear-frequency.dot dump/filterbank-freq.dot
	$(COMPILER)  --unroll 10000000 --frequencyreplacement 1 FilterBank.java
	cp linear-frequency.dot dump/filterbank-freq-only.dot


	$(COMPILER)  --unroll 10000000 --linearreplacement --frequencyreplacement 1 TargetDetect.java
	cp linear.dot dump/target-linear.dot
	cp linear-replace.dot dump/target-linear-replace.dot
	cp linear-frequency.dot dump/target-freq.dot
	$(COMPILER)  --unroll 10000000 --frequencyreplacement 1 TargetDetect.java
	cp linear-frequency.dot dump/target-freq-only.dot


	$(COMPILER)  --unroll 10000000 --linearreplacement --frequencyreplacement 1 FMRadioApp.java
	cp linear.dot dump/fm-linear.dot
	cp linear-replace.dot dump/fm-linear-replace.dot
	cp linear-frequency.dot dump/fm-freq.dot
	$(COMPILER)  --unroll 10000000 --frequencyreplacement 1 FMRadioApp.java
	cp linear-frequency.dot dump/fm-freq-only.dot



dump-beam:
	$(COMPILER)  --unroll 10000000 --linearreplacement --frequencyreplacement 1 CoarseSerializedBeamFormer.java
	cp linear.dot dump/beamformer-linear.dot
	cp linear-replace.dot dump/beamformer-linear-replace.dot
	cp linear-frequency.dot dump/beamformer-freq.dot
	$(COMPILER)  --unroll 10000000 --frequencyreplacement 1 CoarseSerializedBeamFormer.java
	cp linear-frequency.dot dump/beamformer-freq-only.dot





# rules to make the stream graphs for the paper appendix
stream-graphs: clean benchmarks
	mkdir streamgraphs
	$(COMPILER)  FIRProgram.java
	dot -Tps before.dot > streamgraphs/fir.ps
	$(COMPILER)  SamplingRateConverter.java
	dot -Tps before.dot > streamgraphs/samplingrate.ps
	$(COMPILER)  FilterBank.java
	dot -Tps before.dot > streamgraphs/filterbank.ps
	$(COMPILER)  TargetDetect.java
	dot -Tps before.dot > streamgraphs/target.ps
	$(COMPILER)  FMRadioApp.java
	dot -Tps before.dot > streamgraphs/fm.ps
	$(COMPILER)  CoarseSerializedBeamFormer.java
	dot -Tps before.dot > streamgraphs/beamform.ps


fir-scaling: fir-scaling0 fir-scaling1 fir-scaling2
fir-scaling0: Test.java
	./reap_fir.pl 0 | mhmail aalamb@mit.edu -s "(Stupid) FIR scaling results are ready"
fir-scaling1: Test.java
	./reap_fir.pl 1 | mhmail aalamb@mit.edu -s "(Smart) FIR scaling results are ready"
fir-scaling2: Test.java
	./reap_fir.pl 2 | mhmail aalamb@mit.edu -s "() FIR scaling results are ready"

# other benchmarks: CoarseSerializedBeamFormer.java, and FMRadioApp.java

# rule to create java files from .str files
%.java: %.str lib/*.str
	$(FRONTEND) lib/*.str $< > $@

test: Test.java
	$(COMPILER) Test.java > Test-normal.c
	$(COMPILER)  --unroll 10000 --frequencyreplacement Test.java > Test-freq.c
	$(SL) Test-normal.c -o Test-normal.exe
	$(SL) Test-freq.c -o Test-freq.exe

clean:
	rm -f *.dot *.ps *.c *.exe *.class *.output
	rm -f dynamorio.log* countflops.log
	rm -f Test.java
	rm -f FIRProgram.java SamplingRateConverter.java 
	rm -f FilterBank.java TargetDetect.java
	rm -rf streamgraphs