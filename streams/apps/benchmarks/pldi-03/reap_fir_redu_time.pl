#!/usr/local/bin/perl

# this script execites the the TimeTest program with various lengths of FIR filter
# and the redundant replacer to see what the effect of scaling is on
# the execution time of the benchmark.

use strict;
require "reaplib.pl";

my $STANDARD_OPTIONS = "--unroll 100000 --debug";
my $REDU_OPTIONS     = "--redundantreplacement";

# the filename to write out the results to. (append first command line arg to name)
my $RESULTS_FILENAME = "redu_fir_timing.tsv";
my $PROGRAM_NAME = "TimeTest";

# the number of timing tests to run.
my $NUM_TESTS = 5;

# array to hold results
my @result_lines;
# heading
push(@result_lines, 
     "Program\tFIR size\t" .
     "normal outputs\tnormal time\tnormal time no printf\t" .
     "redu outputs\tredu time\tredu time no printf\t" .
     "load 1\t load 5\t load 15\t");

     
     

# generate the java program from the streamit syntax
print `rm -f $PROGRAM_NAME.java`;
print `make $PROGRAM_NAME.java`;

# the number of iterations to run the program for (because we want it to run for a long time)
my $ITERS = 1000000;

my $i;
# for various FIR lengths
my @fir_lengths;
#for ($i=1; $i<32; $i*=sqrt(2)) {
for ($i=64; $i<128; $i++) {
    push(@fir_lengths, int($i));
}

my $firLength;
foreach $firLength (@fir_lengths) {
    # modify the program for the appropriate FIR length
    set_fir_length("$PROGRAM_NAME.java", $firLength);

    # compile normally
    print "normal($firLength):";
    # figure out how many outputs are produced ($NUM_ITERS is defined in reaplib.pl)
    do_compile(".", $PROGRAM_NAME, "$STANDARD_OPTIONS");
    my $normal_outputs = get_output_count(".", $PROGRAM_NAME, $ITERS);
    my $normal_time = time_execution(".", $PROGRAM_NAME, $ITERS);

    # now, recompile the C code after removing the printlines
    remove_prints(".", $PROGRAM_NAME);
    do_c_compile(".", $PROGRAM_NAME);
    my $normal_time_np = time_execution(".", $PROGRAM_NAME, $ITERS);
    print "\n";

    # compile redundant replacement
    print "redu  ($firLength):";
    # figure out how many outputs are produced ($NUM_ITERS is defined in reaplib.pl)
    do_compile(".", $PROGRAM_NAME, "$STANDARD_OPTIONS $REDU_OPTIONS");
    my $redu_outputs = get_output_count(".", $PROGRAM_NAME, $ITERS);
    my $redu_time = time_execution(".", $PROGRAM_NAME, $ITERS);

    # now, recompile the C code after removing the printlines
    remove_prints(".", $PROGRAM_NAME);
    do_c_compile(".", $PROGRAM_NAME);
    my $redu_time_np = time_execution(".", $PROGRAM_NAME, $ITERS);
    print "\n";


    # as a final bit of data, get the load averages
    my ($load_1, $load_5, $load_15) = get_load();


    push(@result_lines, 
	 "$PROGRAM_NAME\t$firLength\t".
	 "$normal_outputs\t$normal_time\t$normal_time_np\t" .
	 "$redu_outputs\t$redu_time\t$redu_time_np\t" .
	 "$load_1\t$load_5\t$load_15");
}


# now, when we are done with all of the tests, write out the results to a tsv file.
print "writing tsv";
open (RFILE, ">$RESULTS_FILENAME");
print RFILE join("\n", @result_lines);
close RFILE;
print "done\n";


