FRONTEND = java streamit.frontend.ToJava
COMPILER = java -Xmx1800M  at.dms.kjc.Main -s

BEAMFORMER-HOME = $(STREAMIT_HOME)/apps/benchmarks/beamformer/streamit
FM-HOME = $(STREAMIT_HOME)/apps/benchmarks/fm/streamit

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



benchmarks: FIRProgram.java SamplingRateConverter.java FilterBank.java TargetDetect.java Test.java CoarseSerializedBeamFormer.java LinkedFMTest.java



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

# rule to make the beamformer application and copy it to this directory
CoarseSerializedBeamFormer.java: $(BEAMFORMER-HOME)/CoarseSerializedBeamFormer.java
	cp $(BEAMFORMER-HOME)/CoarseSerializedBeamFormer.java .
$(BEAMFORMER-HOME)/CoarseSerializedBeamFormer.java:
	make -C $(BEAMFORMER-HOME)
# rule to make the fm radio app and copy it to this directory
LinkedFMTest.java: $(FM-HOME)/LinkedFMTest.java
	cp $(FM-HOME)/LinkedFMTest.java .
$(FM-HOME)/LinkedFMTest.java:
	make -C $(FM-HOME)

# rule to create java files from .str files
%.java: %.str lib/*.str
	$(FRONTEND) lib/*.str $< > $@

test: Test.java
	$(COMPILER) Test.java > Test-normal.c
	$(COMPILER)  --unroll 10000 --frequencyreplacement Test.java > Test-freq.c
	$(SL) Test-normal.c -o Test-normal.exe
	$(SL) Test-freq.c -o Test-freq.exe

clean:
	make -C $(BEAMFORMER-HOME) clean
	rm -f *.dot *.ps *.c *.exe *.class *.output
	rm -f dynamorio.log* countflops.log
	rm -f Test.java
	rm -f FIRProgram.java SamplingRateConverter.java FilterBank.java TargetDetect.java 
	rm -f CoarseSerializedBeamFormer.java LinkedFMTest.java
	rm -rf streamgraphs