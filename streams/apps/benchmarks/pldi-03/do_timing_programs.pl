#!/usr/local/bin/perl
# script that generates times the execution of programs and counts the output
# assumes the existence of programs generates by generate_timing_programs.pl

# Note that files with the postfix of postfix-np merely have had their
# printf's commented out


use strict;
require "reaplib.pl";
my $OUTPUTDIR = "timing";

# input is "postfix:options:filename"

if (not @ARGV) {
    print "usage: do_timing_programs.pl inputscript\n";
    print "  input script has on separate lines: postfix:options:filename\n";
    die();
}

# select the tests to run
my @input_lines = split("\n", read_file(shift(@ARGV)));

# place to put the results line
my @result_lines = ("Name\tOptions\ttime1(sec)\ttime2(sec)\toutputs");

#loop on the tests
foreach (@input_lines) {
    if (not $_) {next;} #ignore blank lines
    my ($postfix, $options, $filename) = split(":");
    
    # if the files don't exist, then signal an error
    if (not (-e "$OUTPUTDIR/$filename-$postfix.exe")) {
	print "$filename-$postfix.exe doesn't exist\n";
	push(@result_lines, "$filename-$postfix\t$options\t0\t0\t0");
	
	next;
    }
    if (not (-e "$OUTPUTDIR/$filename-$postfix-np.exe")) {
	print "$filename-$postfix-np.exe doesn't exist\n";
	push(@result_lines, "$filename-$postfix\t$options\t0\t0\t0");
	next;
    }
    
    print "$filename-$postfix:";
    # run the program for the specified number of iterations until we find a number of iterations
    # that takes greater than a second. Then scale the number up for a total running time
    # of around 20 seconds.
    my $iters = 1;
    my $one_sec_iters = 0; # use to save the number of iters that takes a second (to produce output)
    my $flag = 1; # 0 is true
    while ($flag == 1) {
	my $current_time = time_execution($OUTPUTDIR, "$filename-$postfix-np", $iters);
	if ($current_time > 1) {
	    # remember the number of iters that takes a second
	    $one_sec_iters = $iters;
	    # scale iters to total expected time is 10 seconds
	    $iters = int(20 * ($iters/$current_time));
	    # set the flag that we are done
	    $flag = 0;
	} else {
	    # double the target iterations
	    $iters *= 2;
	}
    }

    print "\nRunning for $iters iterations:";

    # now time the execution for iters
    my $overall_time1 = time_execution($OUTPUTDIR, "$filename-$postfix-np", $iters);
    my $overall_time2 = time_execution($OUTPUTDIR, "$filename-$postfix-np", $iters);
    # now, get the output count for this program 
    my $overall_outputs = get_output_count($OUTPUTDIR, "$filename-$postfix", $iters);
    
    # add a result line
    push(@result_lines, "$filename-$postfix\t$options\t$overall_time1\t$overall_time2\t$overall_outputs");

    # now, run for approximately one 1 milliseconds..
    $one_sec_iters = int($one_sec_iters / 1000);
    if ($one_sec_iters == 0) {$one_sec_iters = 1;} # make sure always more than zero (filterbank...)
    print "\nSaving output of $one_sec_iters iterations...";
    print `$OUTPUTDIR/$filename-$postfix.exe -i $one_sec_iters > $OUTPUTDIR/$filename-$postfix.out`;

    print "done.\n";
}

# write the results to a tsv file
save_tsv("timingResults.tsv", "timing results", @result_lines);
	
