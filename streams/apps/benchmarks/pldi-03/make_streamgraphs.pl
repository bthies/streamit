#!/usr/local/bin/perl
#
# This script generates stream graphs to put in my thesis appendix.
use strict;

my $STREAMIT_COMPILER    = "java -Xmx1500M  at.dms.kjc.Main -s";
my $LINEAR_OPTIONS       = "--unroll 100000 --debug --linearreplacement";

my $result_dir = "streamgraphs";

my @benchmarks = ("FIRProgram",
		  "SamplingRateConverter",
		  "FilterBank",
		  "TargetDetect",
		  "FMRadio",
		  "CoarseSerializedBeamFormer"
		  );
	
# make the benchmarks
print `make benchmarks`;

# make the result directory
print `rm -rf $result_dir`;
print `mkdir $result_dir`;

# for each benchmark program
my $current_program;
foreach $current_program (@benchmarks) {
    # compile the program with the linear options.
    print "$current_program:(compiling)";
    `$STREAMIT_COMPILER $LINEAR_OPTIONS $current_program.java`;
    print "(done)(saving output)";
    # copy the linear and linear-replace dot files into the result directory with different names
    print `cp linear.dot $result_dir/$current_program-linear.dot`;
    print `cp linear-replace.dot $result_dir/$current_program-linear-replace.dot`;
    # all done with this benchmark
    print "(done)\n";
}

# now, we need to convert all of the dot files to ps files
my @dot_files = split("\n", `ls $result_dir/*.dot`);
print "converting dot files:";
my $current_dot_file;
foreach $current_dot_file (@dot_files) {
    print "($current_dot_file)";
    print `dot -Tps $current_dot_file > $current_dot_file.ps`;
    print "(done)";
}
print "\n(all done)\n";


