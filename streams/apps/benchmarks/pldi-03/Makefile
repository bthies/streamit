FRONTEND = java streamit.frontend.ToJava
COMPILER = java -Xmx1800M  at.dms.kjc.Main -s

ANALYSIS-OPTIONS = --debug --linearanalysis --unroll 10000

BEAMFORMER-HOME = $(STREAMIT_HOME)/apps/benchmarks/beamformer/streamit
FM-HOME = $(STREAMIT_HOME)/apps/benchmarks/fm/streamit

all: clean benchmarks

benchmarks: FIRProgram.java \
	    SamplingRateConverter.java \
	    FilterBank.java \
	    TargetDetect.java \
	    TimeTest.java \
	    Test.java \
	    CoarseSerializedBeamFormer.java \
	    LinkedFMTest.java \
	    OneBitDToA.java \
            ChannelVocoder.java




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

clean:
	make -C $(BEAMFORMER-HOME) clean
	rm -f *.dot *.ps *.c *.exe *.class *.output *.data
	rm -f dynamorio.log* countflops.log
	rm -f Test.java BF*.java
	rm -f FIRProgram.java SamplingRateConverter.java FilterBank.java TargetDetect.java 
	rm -f CoarseSerializedBeamFormer.java LinkedFMTest.java OneBitDToA.java
	rm -f ChannelVocoder.java
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


test: ChannelVocoder.java
	jikes ChannelVocoder.java
	java ChannelVocoder -i 10

test-compile: ChannelVocoder.java
	$(COMPILER) --linearanalysis --debug ChannelVocoder.java > ChannelVocoder.c