#!/usr/local/bin/perl

# this script execites the the Test program with various lengths of FIR filter
# and the frequency replacer to see what the effect of scaling is on the
# operation reductions.

use strict;
require "reaplib.pl";

my $STANDARD_OPTIONS = "--unroll 100000 --debug";
my $FREQ_OPTIONS     = "--frequencyreplacement 3";

# the filename to write out the results to. (append first command line arg to name)
my $RESULTS_FILENAME = "freq_fir_results.tsv";
my $PROGRAM_NAME = "Test";

# array to hold results
my @result_lines;
# heading
push(@result_lines, 
     "Program\tFIR size\t" .
     "normal flops\tnormal fadds\tnormal fmuls\tnormal outputs\t" .
     "freq flops\tfreq fadds\tfreq fmuls\tfreq outputs\t");

# generate the java program from the streamit syntax
print `rm -f $PROGRAM_NAME.java`;
print `make $PROGRAM_NAME.java`;


my $i;
# for various FIR lengths
my @fir_lengths;
#for ($i=1; $i<32; $i*=sqrt(2)) {
for ($i=1; $i<128; $i++) {
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
    my ($freq_outputs, $freq_flops, 
	$freq_fadds, $freq_fmuls) = do_test(".", $PROGRAM_NAME, 
						"$STANDARD_OPTIONS $FREQ_OPTIONS",
						"freq  ($firLength)");

    push(@result_lines, 
	 "$PROGRAM_NAME\t$firLength\t".
	 "$normal_flops\t$normal_fadds\t$normal_fmuls\t$normal_outputs\t" .
	 "$freq_flops\t$freq_fadds\t$freq_fmuls\t$freq_outputs\t");
}


# now, when we are done with all of the tests, write out the results to a tsv file.
print "writing tsv";
open (RFILE, ">$RESULTS_FILENAME");
print RFILE join("\n", @result_lines);
close RFILE;
print "done\n";


