#!/usr/local/bin/perl

# this program executes the specified programs
# Using the following options:
# constprop and unroll 10000
# Alone, with linearreplacement, with frequencyreplacement and with both
#
# Then the script executes the program using a dynamo-rio program(module?)
# which counts the number of flops, fadds and fmuls that occur 
# in the program, recording it to a file for analysis.
#
# This is the latest reincarnation of a continually mutating script
# to gather numbers for linear analysis and replacement.
#
# If a filename is passed as an argument, this script will use that as
# the input file specifying which programs to run. If no argument is passed,
# this script just uses the default one.

use strict;
require "reaplib.pl";

# the filename to write out the results to.
my $RESULTS_FILENAME = "freq_results.tsv";

# format for script programs is 
#directory:filename:max size for frequency
my @programs;
# if we have a filename, read it in and use that, otherwise use
if (@ARGV) {
    @programs = split("\n", read_file(shift(@ARGV)));
} else {
    @programs = (
		 ".:FIRProgram",
		 #".:SamplingRateConverter",
		 #".:FilterBank",
		 #".:TargetDetect",
		 #".:LinkedFMTest",
		 #".:CoarseSerializedBeamFormer",
		 #".:OneBitDToA",
		 );
}




# array to hold results
my @result_lines;
# heading
push(@result_lines, 
     "Program\t" .
     "normal flops\tnormal fadds\tnormal fmuls\tnormal outputs\t" .
     "linear flops\tlinear fadds\tlinear fmuls\tlinear outputs\t" .
     "freq 3 flops\tfreq 3 fadds\tfreq 3 fmuls\tfreq 3 outputs\t" .
     "both flops\tboth fadds\tboth fmuls\tboth outputs\t" .
     "redund flops\tredund fadds\tredund fmuls\tredund outputs\t");

# determine the next available results directory (eg results0, results1, etc.)
my $results_dir_num = 0;
while (-e "/tmp/freqResults$results_dir_num") {$results_dir_num++;}
my $results_dir = "/tmp/freqResults$results_dir_num";
print `mkdir $results_dir`;

my $current_program;
foreach $current_program (@programs) {
    #ignore blank lines
   if (not $current_program) {next;}
    # parse the input into path, program and max frequency size
    my ($path, $base_filename) = split(":", $current_program);

    # copy the input program into the new results dir
    print `cp $path/$base_filename.java $results_dir`;

    # update the path
    $path = $results_dir;
    
    # compile normally without frequency replacement
   my ($normal_outputs, $normal_flops, 
       $normal_fadds, $normal_fmuls) = do_test($path, $base_filename,
					       "--unroll 100000 --debug", 
					       "$base_filename(normal)");
   save_output($path, $base_filename, "normal");

   # compile with linear replacement
   my ($linear_outputs, $linear_flops, 
       $linear_fadds, $linear_fmuls) = do_test($path, $base_filename, 
					       "--unroll 100000 --debug --linearreplacement", 
					       "$base_filename(linear)");
   save_output($path, $base_filename, "linear");
   

   # now, do the compilation with (leet fftw) frequency replacement
   my ($freq3_outputs, $freq3_flops, 
       $freq3_fadds, $freq3_fmuls) = do_test($path, $base_filename, 
					     "--unroll 100000 --debug --frequencyreplacement 3",
					     "$base_filename(freq 3)");
   save_output($path, $base_filename, "freq3");
   
   # now, run with both optimizations (fftw and linear)
   my ($both_outputs, $both_flops, 
       $both_fadds, $both_fmuls) = do_test($path, $base_filename, 
					   "--unroll 100000 --debug --linearreplacement --frequencyreplacement 3",
					   "$base_filename(both)");
   save_output($path, $base_filename, "both");

   # now, run with redundant elimination
   my ($redund_outputs, $redund_flops, 
       $redund_fadds, $redund_fmuls) = do_test($path, $base_filename, 
					       "--debug --redundantreplacement",
					       #"--unroll 100000 --debug --redundantreplacement",
					       "$base_filename(redund)");
   save_output($path, $base_filename, "redu");

   
   my $new_data_line = 	     ("$base_filename\t".
			      "$normal_flops\t$normal_fadds\t$normal_fmuls\t$normal_outputs\t" .
			      "$linear_flops\t$linear_fadds\t$linear_fmuls\t$linear_outputs\t" .
			      "$freq3_flops\t$freq3_fadds\t$freq3_fmuls\t$freq3_outputs\t" .
			      "$both_flops\t$both_fadds\t$both_fmuls\t$both_outputs\t" .
			      "$redund_flops\t$redund_fadds\t$redund_fmuls\t$redund_outputs\t");
   
   open (MHMAIL, "|mhmail aalamb\@mit.edu -s \"results mail: ($path,$base_filename)\"");
   print MHMAIL $new_data_line;
   close(MHMAIL);
   
   push(@result_lines, $new_data_line);

}


# now, when we are done with all of the tests, write out the results to a tsv file.
print "(writing tsv)";
open (RFILE, ">$results_dir/$RESULTS_FILENAME");
print RFILE join("\n", @result_lines);
close RFILE;
print "(done)";


print "(sending mail)";
open (MHMAIL, "|mhmail aalamb\@mit.edu -s \"Overall results mail\"");
print MHMAIL join("\n", @result_lines);
close(MHMAIL);
print "(done)\n";



