#!/usr/local/bin/perl

# Run the compiler against the various different programs that 
# we have in this directory using linear analysis.
# we then parse the debugging output and compare the matricies produced against
# expected matricies that are in test files.

use strict;

my @tests = ("regtests/LinearTest1.java",
	     "regtests/LinearTest2.java",
	     "regtests/LinearTest3.java",
	     "regtests/LinearTest4.java",
	     "regtests/LinearTest5.java",
	     "regtests/LinearTest6.java",
	     "regtests/LinearTest7.java",
	     "regtests/LinearTest8.java",
	     "regtests/LinearTest9.str",
	     "regtests/LinearTest10.str",
	     "regtests/LinearTest11.str",
	     "regtests/LinearTest12.str",
	     "regtests/LinearTest13.str",
	     "regtests/LinearTest14.str",);

my $current_test;
foreach $current_test (@tests) {
    # break up the filename into the base and extension
    my ($base, $extension) = split(/\./, $current_test);
    
    # if this is str file, run the frontend to create the appropriate java file
    if ($extension eq "str") {
	print `java streamit.frontend.ToJava < $base.$extension > $base.java`;
    }

    # run the compiler on the java file and save its output
    my $command = ("java -Xmx512M at.dms.kjc.Main -s --constprop --linearanalysis --debug " .
		   "$base.java >& $base.output");
    `$command`;

    # parse the output from the compiler
    $command = "parse_linear_output.pl $base.output > $base.parsed";
    `$command`;

    # compare the parsed output against the expected output
    $command = "cmp $base.parsed $base.expected >& cmp.output";
    `$command`;
    my $result = `cat cmp.output`;
    chomp($result);
    if ($result ne "") {
	print "$base: failure\n";
	print "  $result\n";
    } else {
	print "$base: success\n";
    }
}
