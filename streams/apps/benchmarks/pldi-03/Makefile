FRONTEND = java streamit.frontend.ToJava

all: clean benchmark


benchmark: FIRProgram.java SamplingRateConverter.java FilterBank.java TargetDetect.java Test.java
	./reap_freq.pl


FIRProgram.java: FIRProgram.str lib/*.str
	$(FRONTEND) FIRProgram.str lib/*.str > FIRProgram.java

SamplingRateConverter.java: SamplingRateConverter.str lib/*.str
	$(FRONTEND) SamplingRateConverter.str lib/*.str > SamplingRateConverter.java

FilterBank.java: FilterBank.str lib/*.str
	$(FRONTEND) FilterBank.str lib/*.str > FilterBank.java

TargetDetect.java: TargetDetect.str lib/*.str
	$(FRONTEND) TargetDetect.str lib/*.str > TargetDetect.java


Test.java: Test.str lib/*.str
	$(FRONTEND) Test.str lib/*.str > Test.java





clean:
	rm -f *.dot *.ps *.c *.exe *.class *.output
	rm -f dynamorio.log* countflops.log
	rm -f Test.java
	rm -f FIRProgram.java SamplingRateConverter.java 
	rm -f FilterBank.java TargetDetect.java
