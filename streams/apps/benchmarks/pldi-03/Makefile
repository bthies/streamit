FRONTEND = java streamit.frontend.ToJava

all: clean


FIRProgram.java: FIRProgram.str lib/*.str
	$(FRONTEND) FIRProgram.str lib/*.str > FIRProgram.java

SamplingRateConverter.java: SamplingRateConverter.str lib/*.str
	$(FRONTEND) SamplingRateConverter.str lib/*.str > SamplingRateConverter.java





clean:
	rm -f *.dot *.ps *.c *.exe *.class
	rm -f FIRProgram.java SamplingRateConverter.java
