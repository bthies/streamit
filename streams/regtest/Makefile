CP      = $(CLASSPATH):streamittest/junit.jar:.
JAVA    = java -classpath $(CP) -Dstreamit_home=$(STREAMIT_HOME)
TEXTTEST= $(JAVA) junit.textui.TestRunner
GUITEST = $(JAVA) junit.swingui.TestRunner
COMPILER= jikes -classpath $(CP)
TESTER  = $(TEXTTEST)

all: compile all-tests

all-tests: compile
	$(TESTER) streamittest.TestAll

compile:
	$(COMPILER) streamittest/*.java

fieldprop:
	$(TESTER) streamittest.TestFieldProp

