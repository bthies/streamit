FRONTEND = java streamit.frontend.ToJava
COMPILER = java -Xmx1800M  at.dms.kjc.Main -s

ANALYSIS-OPTIONS = --debug --linearanalysis --unroll 10000

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
fm-results: LinkedFMTest.java
	./reap_freq.pl scripts/fmradio.script
beamformer-results: CoarseSerializedBeamFormer.java
	./reap_freq.pl scripts/beamformer.script
test-results: Test.java
	./reap_freq.pl scripts/test.script



benchmarks: FIRProgram.java SamplingRateConverter.java FilterBank.java TargetDetect.java Test.java CoarseSerializedBeamFormer.java LinkedFMTest.java OneBitDToA.java





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
	rm -f Test.java BF*.java
	rm -f FIRProgram.java SamplingRateConverter.java FilterBank.java TargetDetect.java 
	rm -f CoarseSerializedBeamFormer.java LinkedFMTest.java
	rm -rf streamgraphs

report_clean:
	rm -rf linear_report.* workingfiles

report: report_clean
	./harvest_linearity.pl
	latex linear_report.tex
	latex linear_report.tex
	dvips -Ppdf linear_report.dvi -o linear_report.ps
	ps2pdf linear_report.ps

#redundancy data gathering
redundant-fir: benchmarks
	$(COMPILER) $(ANALYSIS-OPTIONS) FIRProgram.java > redundant/fir.output
	./parse_redundant_output.pl redundant/fir.output
redundant-target: benchmarks
	$(COMPILER) $(ANALYSIS-OPTIONS) TargetDetect.java > redundant/target.output
	./parse_redundant_output.pl redundant/target.output
redundant-filterbank: benchmarks
	$(COMPILER) $(ANALYSIS-OPTIONS) FilterBank.java > redundant/filterbank.output
	./parse_redundant_output.pl redundant/filterbank.output
redundant-sampling: benchmarks
	$(COMPILER) $(ANALYSIS-OPTIONS) SamplingRateConverter.java > redundant/sample.output
	./parse_redundant_output.pl redundant/sample.output
redundant-fm: benchmarks
	$(COMPILER) $(ANALYSIS-OPTIONS) LinkedFMTest.java > redundant/fm.output
	./parse_redundant_output.pl redundant/fm.output

redundant-beamformer: benchmarks
	$(COMPILER) $(ANALYSIS-OPTIONS) CoarseSerializedBeamFormer.java > redundant/beam.output
	./parse_redundant_output.pl redundant/beam.output




current-test: OneBitDToA.java
	jikes OneBitDToA.java
	#$(COMPILER) OneBitDToA.java > OneBitDToA.c
	java OneBitDToA -i 1000 > output.tsv