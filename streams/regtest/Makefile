CP      = $(CLASSPATH):streamittest/junit.jar:.
JAVA    = java -Xmx1700M -classpath $(CP) -Dstreamit_home=$(STREAMIT_HOME)
TEXTTEST= $(JAVA) junit.textui.TestRunner
GUITEST = $(JAVA) junit.swingui.TestRunner
COMPILER= jikes -classpath $(CP)

# set up which tester gets used
#TESTER  = $(GUITEST)
TESTER  = $(TEXTTEST)



# if automatic_test is definied (by another makefile or the cron job script
# make sure that the text tester is always used
ifneq ($(AUTOMATIC_TEST),)
  TESTER  = $(TEXTTEST)
endif





all: compile test-all

# test run all applications, with all combos of optimizations (takes a looong time)
test-all: compile
	$(TESTER) streamittest.TestAll

# create a report (with performance numbers) from last regression run
report:
	tools/parse_results.pl regtest_log.txt regtest_errors.txt regtest_success.txt regtest_results.txt



# run all tests with no optimizations
test-once: compile
	$(TESTER) streamittest.TestOnce

# run testbed (scratch work space)
test-bed: compile
	$(TESTER) streamittest.TestBed

# run "tests"
test-tests: compile
	$(TESTER) streamittest.TestTests
# run application tests
test-apps: compile
	$(TESTER) streamittest.TestApps
# run example tests
test-examples: compile
	$(TESTER) streamittest.TestExamples
# run benchmark tests
test-benchmarks: compile
	$(TESTER) streamittest.TestBenchmarks


#checkin tests
test-checkin: compile do-test-checkin report
test-checkin-raw: compile do-test-checkin-raw report	

do-test-checkin:
	$(TESTER) streamittest.TestCheckin

do-test-checkin-raw: compile
	$(TESTER) streamittest.TestCheckinRaw


# linear framework tests (lower level than the regression
# testing framework for testing the entire compiler path.
test-linear: compile
	jikes /u/aalamb/streams/compiler/kopi/kjc/sir/linear/*.java
	$(TESTER) streamittest.TestLinear



# Base functionality
compile:
	$(COMPILER) streamittest/*.java
	rm -f ../library/c/libstreamit.a
	$(MAKE) -C ../library/c

clean:
	rm -rf streamittest/*.class
	rm -rf *.dot
	rm -rf regtest_errors.txt