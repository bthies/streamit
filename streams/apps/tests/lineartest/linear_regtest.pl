#!/usr/local/bin/perl

# Run the compiler against the various different programs that 
# we have in this directory using linear analysis.
# we then parse the debugging output and compare the matricies produced against
# expected matricies that are in test files.

use strict;
# java(streamit) --> c
my $S = "java -Xmx512M at.dms.kjc.Main -s --unroll 100000 --debug ";
# c --> exe
my $SL = "gcc -O2 -I/u/aalamb/streams/library/c -L/u/aalamb/streams/library/c/";
my $SL_POST = " -lstreamit -lsrfftw -lsfftw -lm ";
# compare output
my $CMP = "/u/aalamb/streams/regtest/tools/compare_uni.pl";
my $CMP_PATH = "/u/aalamb/streams/regtest/tools/";

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
	     );

@tests = ("regtests/LinearTest14.str");


my $current_test;
foreach $current_test (@tests) {
    # break up the filename into the base and extension
    my ($base, $extension) = split(/\./, $current_test);
    
    # if this is str file, run the frontend to create the appropriate java file
    if ($extension eq "str") {
	print `java streamit.frontend.ToJava --full $base.$extension > $base.java`;
    }

    # run the compiler on the java file and save its output
    my $command = ("$S --linearanalysis " .
		   "$base.java >& $base.c");
    `$command`;

    # parse the output from the compiler
    $command = "parse_linear_output.pl $base.c > $base.parsed";
    `$command`;

    # compare the parsed output against the expected output
    my $result = `cmp $base.parsed $base.expected`;
    chomp($result);
    if ($result ne "") {
	print "$base(analysis): failure\n";
	print "  $result\n";
    } else {
	print "$base(analysis): success\n";
    }

    # now, compile the test again, this time with linear replacement enabled
    $command = ("$S --linearreplacement " .
		"$base.java >& $base.replaced.c");
    `$command`;
    
    # now, compile the c to an exe for both the original program and the replaced prgram
    print `$SL $base.c -o $base.exe $SL_POST`;
    print `$SL $base.replaced.c -o $base.replaced.exe $SL_POST`;
    # execute both the original and the replaced program 100 iterations
    print `$base.exe -i 100 > $base.run`;
    print `$base.replaced.exe -i 100 > $base.replaced.run`;
    # now, compare the outputs
    $result = `perl -I$CMP_PATH $CMP $base.run $base.replaced.run`;
    chomp($result);
    if ($result ne "") {
	print "$base(replace):  failure\n";
	print "  $result\n";
    } else {
	print "$base(replace):  success\n";
    }

    
    
}
