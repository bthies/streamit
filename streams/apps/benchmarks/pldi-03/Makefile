FRONTEND = java streamit.frontend.ToJava
COMPILER = java -Xmx1800M  at.dms.kjc.Main -s

ANALYSIS-OPTIONS = --debug --linearanalysis --unroll 10000

BEAMFORMER-HOME = $(STREAMIT_HOME)/apps/benchmarks/beamformer/streamit
FM-HOME = $(STREAMIT_HOME)/apps/benchmarks/fm/streamit

SCRIPTS = scripts/fir.script \
	  scripts/sample.script \
	  scripts/target.script \
	  scripts/fm.script \
	  scripts/fb.script \
	  scripts/vocoder.script \
	  scripts/oversamp.script \
	  scripts/onebit.script \
	  scripts/bf.script \
	  scripts/bf2.script \

SG-TARGET = $(HOME)/streams/docs/pldi-03-linear/streamgraphs

all: clean benchmarks

benchmarks: FIRProgram.java \
	    SamplingRateConverter.java \
	    FilterBank.java \
	    TargetDetect.java \
	    TimeTest.java \
	    Test.java \
	    CoarseSerializedBeamFormer.java \
	    FMRadio.java \
	    OneBitDToA.java \
            ChannelVocoder.java \
	    Oversampler.java




# rule to make the beamformer application and copy it to this directory
CoarseSerializedBeamFormer.java: $(BEAMFORMER-HOME)/CoarseSerializedBeamFormer.str
	$(FRONTEND) $(BEAMFORMER-HOME)/CoarseSerializedBeamFormer.str > $@
$(BEAMFORMER-HOME)/CoarseSerializedBeamFormer.str:
	make -C $(BEAMFORMER-HOME)

# rule to create java files from .str files
%.java: %.str lib/*.str
	$(FRONTEND) lib/*.str $< > $@

clean:
	make -C $(BEAMFORMER-HOME) clean
	rm -f *.dot *.ps *.c *.exe *.class *.output *.data
	rm -f dynamorio.log* countflops.log
	rm -f Test.java BF*.java
	rm -f FIRProgram.java SamplingRateConverter.java FilterBank.java TargetDetect.java 
	rm -f CoarseSerializedBeamFormer.java FMRadio.java OneBitDToA.java
	rm -f ChannelVocoder.java
	rm -rf streamgraphs scripts/overall.script

report_clean:
	rm -rf linear_report.* workingfiles

report: report_clean
	./harvest_linearity.pl
	latex linear_report.tex
	latex linear_report.tex
	dvips -Ppdf linear_report.dvi -o linear_report.ps
	ps2pdf linear_report.ps

overall-script:
	cat $(SCRIPTS) > scripts/overall.script


test: ChannelVocoder.java
	jikes ChannelVocoder.java
	java ChannelVocoder -i 10

test-compile: ChannelVocoder.java
	$(COMPILER) --linearanalysis --debug ChannelVocoder.java > ChannelVocoder.c

cpsg:
	cp timing/FIRProgram-linear-linear-simple.dot $(SG-TARGET)
	cp timing/SamplingRateConverter-linear-linear-simple.dot $(SG-TARGET)
	cp timing/TargetDetect-linear-linear-simple.dot $(SG-TARGET)
	cp timing/FMRadio-linear-linear-simple.dot $(SG-TARGET)
	cp timing/CoarseSerializedBeamFormer-2linear-linear-simple.dot $(SG-TARGET)
	cp timing/FilterBank-linear-linear-simple.dot $(SG-TARGET)
	cp timing/ChannelVocoder-linear-linear-simple.dot $(SG-TARGET)
	cp timing/Oversampler-linear-linear-simple.dot $(SG-TARGET)
	cp timing/OneBitDToA-linear-linear-simple.dot $(SG-TARGET)

numbers:
	./do_generate_programs.pl scripts/quick-few.script 
	./do_timing_programs.pl scripts/quick-few.script 
	./do_operation_programs.pl scripts/quick-few.script 
#	./do_correctness_programs.pl scripts/quick-dtoa.script
#	./do_correctness_programs.pl scripts/quick-filterbank.script
#	./do_correctness_programs.pl scripts/quick-fir.script
#	./do_correctness_programs.pl scripts/quick-fm.script
#	./do_correctness_programs.pl scripts/quick-oversampler.script
#	./do_correctness_programs.pl scripts/quick-rateconvert.script
#	./do_correctness_programs.pl scripts/quick-targetdetect.script
#	./do_correctness_programs.pl scripts/quick-vocoder.script


