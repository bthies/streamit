CP      = $(CLASSPATH):streamittest/junit.jar:.
JAVA    = java -Xmx1700M -classpath $(CP) -Dstreamit_home=$(STREAMIT_HOME)
TEXTTEST= $(JAVA) junit.textui.TestRunner
GUITEST = $(JAVA) junit.swingui.TestRunner
COMPILER= jikes -classpath $(CP)
UI	= text

# set up which tester gets used
ifeq ($(UI),gui)
TESTER  = $(GUITEST)
else
ifeq ($(UI),swing)
TESTER	= $(GUITEST)
else
TESTER  = $(TEXTTEST)
endif
endif

# if automatic_test is definied (by another makefile or the cron job script
# make sure that the text tester is always used
ifneq ($(AUTOMATIC_TEST),)
  TESTER  = $(TEXTTEST)
endif





all: compile test-all

# test run all applications, with all combos of optimizations (takes a looong time -- like ~20 hours)
test-all: compile
	$(TESTER) streamittest.TestAll

# runs the nightly regtest (doesn't do raw 8 so it takes a lot less time)
test-nightly: compile
	$(TESTER) streamittest.TestNightly


# create a report (with performance numbers) from last regression run
report:
	tools/parse_results.pl regtest_log.txt regtest_errors.txt regtest_success.txt regtest_results.txt



# run tests to stress partitioning and scaling
test-partition: compile
	$(TESTER) streamittest.TestPartition

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
	$(TESTER) streamittest.TestOnlyExamples
# run benchmark tests
test-benchmarks: compile
	$(TESTER) streamittest.TestOnlyBenchmarks


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
	#make -C $(STREAMIT_HOME)/compiler
	jikes -nowarn ~/linear/*.java
	$(TESTER) streamittest.TestLinear


# test the pldi-03 benchmarks (eg the ones Andrew used for the
# Linear optimizations.
test-pldi: compile
	make -C $(STREAMIT_HOME)/compiler
	$(TESTER) streamittest.TestPldi03Benchmarks



# Base functionality -- build the regression test library,
# rebuild the c library (for uniprocessor support).
compile:
	$(COMPILER) streamittest/*.java
	rm -f ../library/c/libstreamit.a
	$(MAKE) -C ../library/c

clean:
	rm -rf streamittest/*.class
	rm -rf *.dot
	rm -rf regtest_errors.txt regtest_perf_test.txt regtest_success.txt regtest_log.txt 


