CP      = $(CLASSPATH):streamittest/junit.jar:.
JAVA    = java -classpath $(CP) -Dstreamit_home=$(STREAMIT_HOME)
TEXTTEST= $(JAVA) junit.textui.TestRunner
GUITEST = $(JAVA) junit.swingui.TestRunner
COMPILER= jikes -classpath $(CP)

# use to determine which tester gets used
#TESTER  = $(GUITEST)
TESTER  = $(TEXTTEST)

all: compile all-tests

all-tests: compile
	$(TESTER) streamittest.TestAll

app-tests: compile
	$(TESTER) streamittest.TestApps

compile:
	$(COMPILER) streamittest/*.java

clean:
	rm -rf streamittest/*.class
	rm -rf *.dot
	rm -rf regtest_errors.txt