#!/usr/local/bin/perl

# this program executes the specified programs
# Using the following options:
# constprop and unroll 10000
# Alone, with linearreplacement, with frequencyreplacement, with both,
# and with redundant replacement
#
# Then the script executes the program using a dynamo-rio program(module?)
# which counts the number of flops, fadds and fmuls that occur 
# in the program, recording it to a tsv file for analysis.
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
# directory:filename
my @programs;
# if we have a filename, read it in and use that, otherwise use
if (@ARGV) {
    @programs = split("\n", read_file(shift(@ARGV)));
} else {
    @programs = (
		 #".:Test",
		 ".:FIRProgram",
		 #".:SamplingRateConverter",
		 #".:FilterBank",
		 #".:TargetDetect",
		 #".:FMRadio",
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
     "linpart flops\tlinpart fadds\tlinpart fmuls\tlinpart outputs\t");

# determine the next available results directory (eg results0, results1, etc.)
my $results_dir_num = 0;
while (-e "/tmp/freqResults$results_dir_num") {$results_dir_num++;}
my $results_dir = "/tmp/freqResults$results_dir_num";
print `mkdir $results_dir 2> /dev/null`;

my $current_program;
foreach $current_program (@programs) {
    # ignore blank lines
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
    print "\n";

    # compile with linear replacement
    my ($linear_outputs, $linear_flops, 
	$linear_fadds, $linear_fmuls) = do_test($path, $base_filename, 
						"--unroll 100000 --debug --linearreplacement", 
						"$base_filename(linear)");
    print "\n";
   

    # now, do the compilation with (leet fftw) frequency replacement
    my ($freq3_outputs, $freq3_flops, 
	$freq3_fadds, $freq3_fmuls) = do_test($path, $base_filename, 
					      "--unroll 100000 --debug --frequencyreplacement",
					      "$base_filename(freq 3)");
    print "\n";
   
    # now, run with both optimizations (fftw and linear)
    my ($both_outputs, $both_flops, 
	$both_fadds, $both_fmuls) = do_test($path, $base_filename, 
					    "--unroll 100000 --debug --linearreplacement --frequencyreplacement",
					    "$base_filename(both)");
    print "\n";

    # now, run with linear partitioning
    my ($linpart_outputs, $linpart_flops, 
	$linpart_fadds, $linpart_fmuls) = do_test($path, $base_filename, 
						  "--debug --unroll 100000 --linearpartition",
						  "$base_filename(linpart)");
    print "\n";
    

    my $new_data_line = ("$base_filename\t".
			 "$normal_flops\t$normal_fadds\t$normal_fmuls\t$normal_outputs\t" .
			 "$linear_flops\t$linear_fadds\t$linear_fmuls\t$linear_outputs\t" .
			 "$freq3_flops\t$freq3_fadds\t$freq3_fmuls\t$freq3_outputs\t" .
			 "$both_flops\t$both_fadds\t$both_fmuls\t$both_outputs\t" .
			 "$linpart_flops\t$linpart_fadds\t$linpart_fmuls\t$linpart_outputs\t");
    # send intermediary results to andrew
    open (MHMAIL, "|mhmail \$USER\@cag.lcs.mit.edu -s \"results line mail: ($path,$base_filename)\"");
    print MHMAIL $new_data_line;
    close(MHMAIL);

    # save the data lines into the two arrays
    push(@result_lines, $new_data_line);
}



# now, when we are done with all of the tests, write out the results to a tsv file.
save_tsv("$results_dir/$RESULTS_FILENAME", "Optimization Results", @result_lines);
print "(done)\n"



