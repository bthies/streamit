#!/usr/local/bin/perl

# Run the compiler against the various different programs that 
# we have in this directory using linear analysis.
# we then parse the debugging output and compare the matricies produced against
# expected matricies that are in test files.

use strict;
# java(streamit) --> exe
my $S = "strc ";
# compare output
my $CMP = "../../../regtest/tools/compare_uni.pl";
my $CMP_PATH = "../../../regtest/tools/";

my @tests = (
	     "regtests/LinearTest1.java",
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
	     "regtests/LinearTest14.str",
	     "regtests/LinearTest15.str",
	     "regtests/LinearTest16.str",
	     "regtests/LinearTest17.str",
	     "regtests/LinearTest18.str",
	     "regtests/LinearTest19.str",
	     "regtests/LinearTest20.str",
	     "regtests/LinearTest21.str",
	     "regtests/LinearTest22.str",
	     "regtests/LinearTest23.str",
	     "regtests/LinearTest24.str",
	     "regtests/LinearTest25.str",
	     "regtests/LinearTest26.str",
	     "regtests/LinearTest27.str",
	     "regtests/LinearTest28.str",
	     "regtests/LinearTest29.str",
	     "regtests/LinearTest30.str",
	     "regtests/LinearTest31.str",
	     "regtests/LinearTest32.str",
	     "regtests/LinearTest33.str",
	     "regtests/LinearTest34.str",
	     "regtests/LinearTest35.str",
	     "regtests/LinearTest36.str",
	     "regtests/LinearTest37.str",
	     );

#@tests = (
#	  "regtests/LinearTest37.str",
#	  );


my $current_test;
foreach $current_test (@tests) {
    # break up the filename into the base and extension
    my ($base, $extension) = split(/\./, $current_test);

    print "\nTesting $base -------------------------------------\n";
    
    # if this is str file, run the frontend to create the appropriate java file
    if ($extension eq "str") {
	print `java streamit.frontend.ToJava --full $base.$extension > $base.java`;
    }

    print "\nBase:\n";
    # run the compiler on the java file (no linear stuff)
    my $command = ("$S " .
		   "$base.java -o $base.exe");
    `$command`;

    print "\nLinear replacement:\n";
    # now, compile the test again, this time with linear replacement enabled.  save its output to compare
    print `cp $base.java $base.replaced.java`;
    $command = ("$S --linearreplacement " .
		"$base.replaced.java -o $base.replaced.exe");
    `$command`;

    # parse the output from the compiler -- disabled
    #$command = "parse_linear_output.pl $base.replaced.c > $base.replaced.parsed";
    #`$command`;

    # compare the parsed output against the expected output
    #my $result = `cmp $base.replaced.parsed $base.expected`;
    #chomp($result);
    #if ($result ne "") {
#	print "$base(analysis): failure\n";
#	print "  $result\n";
#    } else {
#	print "$base(analysis): success\n";
#    }

    # now, compile the test again, this time with frequency replacement enabled
    print "\nFrequency replacement:\n";
    $command = ("$S --frequencyreplacement " .
		"$base.java -o $base.freq.exe");
    `$command`;

    # now, compile the test again, this time with statespace replacement enabled
    print "\nStatespace replacement:\n";
    $command = ("$S --statespace --linearreplacement " .
		"$base.java -o $base.statespace.exe");
    `$command`;

    # now, compile the test one last time, with redundancy replacement enabled
    #$command = ("$S --redundantreplacement " .
	#	"$base.java -o $base.redund.exe");
    #`$command`;

    print "\nChecking correctness:\n";
    # execute both the original and the replaced program 100 iterations
    print `$base.exe -i 100 > $base.run`;
    print `$base.replaced.exe -i 100 > $base.replaced.run`;
    print `$base.freq.exe -i 100 > $base.freq.run`;
    print `$base.statespace.exe -i 100 > $base.statespace.run`;
    #print `$base.redund.exe -i 100 > $base.redund.run`;

    # now, compare replacement to normal
    my $result = `perl -I$CMP_PATH $CMP $base.run $base.replaced.run`;
    chomp($result);
    if ($result ne "") {
	print "$base(replace):        failure\n";
	print "  $result\n";
    } else {
	print "$base(replace):        success\n";
    }

    # now, compare frequency to normal
    $result = `perl -I$CMP_PATH $CMP $base.run $base.freq.run`;
    chomp($result);
    if ($result ne "") {
	print "$base(freq):           failure\n";
	print "  $result\n";
    } else {
	print "$base(freq):           success\n";
    }

    # now, compare statespace to normal
    $result = `perl -I$CMP_PATH $CMP $base.run $base.statespace.run`;
    chomp($result);
    if ($result ne "") {
	print "$base(statespace):     failure\n";
	print "  $result\n";
    } else {
	print "$base(statespace):     success\n";
    }

    # now, compare redundancy to normal
    #$result = `perl -I$CMP_PATH $CMP $base.run $base.redund.run`;
    #chomp($result);
    #if ($result ne "") {
#	print "$base(redund):   failure\n";
#	print "  $result\n";
#    } else {
#	print "$base(redund):   success\n";
#    }

    
    
}
