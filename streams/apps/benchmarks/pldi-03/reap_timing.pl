#!/usr/local/bin/perl

# this program executes the specified programs
# Using the following options:
# constprop and unroll 10000
# Alone, with linearreplacement, with frequencyreplacement, with both and with redundant replacement
#
# and then it times each resulting program
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
my $TIMING_FILENAME  = "timing_results.tsv";

# format for script programs is 
#directory:filename:max size for frequency
my @programs;
# if we have a filename, read it in and use that, otherwise use
if (@ARGV) {
    @programs = split("\n", read_file(shift(@ARGV)));
} else {
    @programs = (
		 #dir:Filename:normaliters:freqiters",
		 #".:Test:1000",
		 ".:FIRProgram:1000000000:100000",
		 #".:SamplingRateConverter:1000",
		 #".:FilterBank:1000",
		 #".:TargetDetect:1000",
		 #".:LinkedFMTest:1000",
		 #".:CoarseSerializedBeamFormer",
		 #".:OneBitDToA:1000",
		 );
}




# array to hold timing results
my @timing_lines;
push(@timing_lines,
     "Program\t" .
     "normal outputs\tnormal time 1\tnormal time 2\tnormal time 3\t" .
     "linear outputs\tlinear time 1\tlinear time 2\tlinear time 3\t" .
     "freq 3 outputs\tfreq 3 time 1\tfreq 3 time 2\tfreq 3 time 3\t" .
     "both outputs\tboth time 1\tboth time 2\tboth time 3\t" .
     "redund outputs\tredund time 1\tredund time 2\tredund time 3\t");
     

# determine the next available results directory (eg results0, results1, etc.)
my $results_dir_num = 0;
while (-e "/tmp/timeResults$results_dir_num") {$results_dir_num++;}
my $results_dir = "/tmp/timeResults$results_dir_num";
print `mkdir $results_dir 2> /dev/null`;

my $current_program;
foreach $current_program (@programs) {
    # ignore blank lines
    if (not $current_program) {next;}

    # parse the input into path, program and max frequency size
    my ($path, $base_filename, $normal_iters, $freq_iters) = split(":", $current_program);
    if ((not $normal_iters) or (not $freq_iters)) {
	die("no iteration count specified for: ($path)($base_filename)");
    }
    
    # copy the input program into the new results dir
    print `cp $path/$base_filename.java $results_dir`;

    # update the path
    $path = $results_dir;
    
    print("$base_filename(normal):");
    do_compile($path, $base_filename, "--unroll 100000 --debug");
    my $normal_outputs = get_output_count($path, $base_filename, $normal_iters);
	
    # remove the printfs for timing information
    remove_prints($path, $base_filename);
    do_c_compile($path, $base_filename);
    # time the execution of this program
    my $normal_time_1 = time_execution($path, $base_filename, $normal_iters);
    my $normal_time_2 = time_execution($path, $base_filename, $normal_iters);
    my $normal_time_3 = time_execution($path, $base_filename, $normal_iters);
    print "\n";
    
    # compile with linear replacement
    print "$base_filename(linear):";
    do_compile($path, $base_filename, "--unroll 100000 --debug --linearreplacement");
    my $linear_outputs = get_output_count($path, $base_filename, $normal_iters);
    
    # remove the printfs for timing information
    remove_prints($path, $base_filename);
    do_c_compile($path, $base_filename);
    # time the execution of this program
    my $linear_time_1 = time_execution($path, $base_filename, $normal_iters);
    my $linear_time_2 = time_execution($path, $base_filename, $normal_iters);
    my $linear_time_3 = time_execution($path, $base_filename, $normal_iters);
    print "\n";
   

    # now, do the compilation with (leet fftw) frequency replacement
    print "$base_filename(freq):";
    do_compile($path, $base_filename, "--unroll 100000 --debug --frequencyreplacement");
    my $freq3_outputs = get_output_count($path, $base_filename, $freq_iters);
	
    # remove the printfs for timing information
    remove_prints($path, $base_filename);
    do_c_compile($path, $base_filename);
    # time the execution of this program
    my $freq3_time_1 = time_execution($path, $base_filename, $freq_iters);
    my $freq3_time_2 = time_execution($path, $base_filename, $freq_iters);
    my $freq3_time_3 = time_execution($path, $base_filename, $freq_iters);
    print "\n";

   
    # now, run with both optimizations (fftw and linear)
    print "$base_filename(both):";
    do_compile($path, $base_filename, "--unroll 100000 --debug --linearreplacement --frequencyreplacement");
    my $both_outputs = get_output_count($path, $base_filename, $freq_iters);

    # remove the printfs for timing information
    remove_prints($path, $base_filename);
    do_c_compile($path, $base_filename);
    # time the execution of this program
    my $both_time_1 = time_execution($path, $base_filename, $freq_iters);
    my $both_time_2 = time_execution($path, $base_filename, $freq_iters);
    my $both_time_3 = time_execution($path, $base_filename, $freq_iters);
    print "\n";

    # now, run with redundant elimination
#    print "$base_filename(redund):";
#    do_compile($path, $base_filename, "--debug --unroll 100000 --redundantreplacement");
#    my $redund_outputs = get_output_count($path, $base_filename, $freq_iters);
#
#    # remove the printfs for timing information
#    remove_prints($path, $base_filename);
#    do_c_compile($path, $base_filename);
#    # time the execution of this program
#    my $redund_time_1 = time_execution($path, $base_filename, $freq_iters);
#    my $redund_time_2 = time_execution($path, $base_filename, $freq_iters);
#    my $redund_time_3 = time_execution($path, $base_filename, $freq_iters);
#    print "\n";

# fake this part for now. Otherwise it takes too long to run these tests"
my ($redund_outputs, $redund_flops, $redund_fadds, $redund_fmuls,
    $redund_time_1, $redund_time_2, $redund_time_3) = (0,0,0,0,0,0,0);


    my $new_timing_line=("$base_filename\t" .
			 "$normal_outputs\t$normal_time_1\t$normal_time_2\t$normal_time_3\t" .
			 "$linear_outputs\t$linear_time_1\t$linear_time_2\t$linear_time_3\t" .
			 "$freq3_outputs\t$freq3_time_1\t$freq3_time_2\t$freq3_time_3\t" .
			 "$both_outputs\t$both_time_1\t$both_time_2\t$both_time_3\t" .
			 "$redund_outputs\t$redund_time_1\t$redund_time_2\t$redund_time_3\t");
			 
    # send intermediary results to andrew
    open (MHMAIL, "|mhmail aalamb\@mit.edu -s \"timing results line mail: ($path,$base_filename)\"");
    print MHMAIL $new_timing_line;    
    close(MHMAIL);

    # save the data lines into the two arrays
    push(@timing_lines, $new_timing_line);

}



# now, when we are done with all of the tests, write out the results to a tsv file.
save_tsv("$results_dir/$TIMING_FILENAME", "Timing Results", @timing_lines);
print "(done)\n"



