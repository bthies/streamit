#!/usr/local/bin/perl

# this script execites the the Test program with various lengths of FIR filter
# and the linear redundancy replacer to see what the effect of scaling is on the
# operation reductions.

use strict;
require "reaplib.pl";

my $STANDARD_OPTIONS = "--unroll 100000 --debug";
my $REDU_OPTIONS     = "--redundantreplacement";

# the filename to write out the results to. (append first command line arg to name)
my $RESULTS_FILENAME = "redu_fir_results.tsv";

my $PROGRAM_NAME = "Test";

# array to hold results
my @result_lines;
# heading
push(@result_lines, 
     "Program\tFIR size\t" .
     "normal flops\tnormal fadds\tnormal fmuls\tnormal outputs\t" .
     "redu flops\tredu fadds\tredu fmuls\tredu outputs\t");

# generate the java program from the streamit syntax
print `rm -f $PROGRAM_NAME.java`;
print `make $PROGRAM_NAME.java`;


my $i;
my @fir_lengths;
# for various FIR lengths
for ($i=1; $i<2; $i++) {
    push(@fir_lengths, int($i));
}

my $firLength;
foreach $firLength (@fir_lengths) {
    # modify the program for the appropriate FIR length
    set_fir_length("$PROGRAM_NAME.java", $firLength);

    # compile normally
    my ($normal_outputs, $normal_flops, 
	$normal_fadds, $normal_fmuls) = do_test(".", $PROGRAM_NAME, 
						"$STANDARD_OPTIONS",
						"normal($firLength)");

    # compile with frequency replacement
    my ($redu_outputs, $redu_flops, 
	$redu_fadds, $redu_fmuls) = do_test(".", $PROGRAM_NAME, 
						"$STANDARD_OPTIONS $REDU_OPTIONS",
						"redu  ($firLength)");



    push(@result_lines, 
	 "$PROGRAM_NAME\t$firLength\t".
	 "$normal_flops\t$normal_fadds\t$normal_fmuls\t$normal_outputs\t" .
	 "$redu_flops\t$redu_fadds\t$redu_fmuls\t$redu_outputs\t");
}


# now, when we are done with all of the tests, write out the results to a tsv file.
print "writing tsv";
open (RFILE, ">$RESULTS_FILENAME");
print RFILE join("\n", @result_lines);
close RFILE;
print "done\n";


