#!/usr/local/bin/perl

# Run the compiler against the various different programs that 
# we have in this directory using linear analysis.
# we then parse the debugging output and compare the matricies produced against
# expected matricies that are in test files.

use strict;

my @tests = ("regtests/LinearTest1",
	     "regtests/LinearTest2",
	     "regtests/LinearTest3",
	     "regtests/LinearTest4",
	     "regtests/LinearTest5",
	     "regtests/LinearTest6",
	     "regtests/LinearTest7",);

my $current_test;
foreach $current_test (@tests) {
    # run the compiler and save its output
    my $command = ("java -Xmx512M at.dms.kjc.Main -s --constprop --linearanalysis --debug " .
		   "$current_test.java >& $current_test.output");
    `$command`;

    # parse the output from the compiler
    $command = "parse_linear_output.pl $current_test.output > $current_test.parsed";
    `$command`;

    # compare the parsed output against the expected output
    $command = "cmp $current_test.parsed $current_test.expected >& cmp.output";
    `$command`;
    my $result = `cat cmp.output`;
    chomp($result);
    if ($result ne "") {
	print "$current_test: failure\n";
	print "  $result\n";
    } else {
	print "$current_test: success\n";
    }
}
