#!/usr/local/bin/perl

# Script to parse the results of running the regression test framework
# and generate a nice summary of the results that will be sent to 
# the commit group.
#
# Usage: parse_results log_file.txt error_file.txt success_file.txt results_file.txt
# (where log_file is the output from running make in streams/regtest,
#  error_file is the file containing the error messages from the tests, 
#  success_file is the file containing the successful regression tests, and
#  results_file contains the results (eg speed) of running the various 
#   different configurations of the compiler.)
#
# Note: The contents of the error file are not acutally used at the present time. 
#  error messages are parsed out of the log file instead.
#
# $Id: parse_results.pl,v 1.4 2002-07-03 19:31:51 aalamb Exp $

use strict;

# entry point
main();


sub main {
    my $log_filename     = shift(@ARGV) || die ("No log filename supplied\n");
    my $error_filename   = shift(@ARGV) || die ("No error filename supplied\n");
    my $success_filename   = shift(@ARGV) || die ("No success filename supplied\n");
    my $results_filename   = shift(@ARGV); # don't generate result summary if filename not supplied


    # read in the contents of the log, error, and success files
    my $log_contents     = read_file($log_filename);
    my $error_contents   = read_file($error_filename); 
    my $success_contents = read_file($success_filename);

    # generate an executive summary of the results of the regression tests
    generate_executive_summary($log_contents);
    # generate a failure report
    generate_failure_report($log_contents, $error_contents, $success_contents);

    print "\n\n";

    # Generate a report of the raw tests whose performance has changed since the last run
    # if the results file is included
    if ($results_filename) {
	generate_performance_report($results_filename);
    }


}

sub generate_executive_summary {
    my $log = shift || die ("no log passed");

    # find the time it took to run the test, and the overall result (next line)
    my @parsed   = $log =~ m/Time: (.*)\n*(.*)/gi;
    my $run_time = shift(@parsed);
    my $result   = shift(@parsed);

    # remove any commas in the runtime
    $run_time =~ s/,//gi;

    print "StreamIT Regression Test Results\n";
    print `date`;
    print "Execution Time: " . $run_time / 60 . " minutes.\n";
    print "Summary: $result\n";
}

sub generate_failure_report {
    my $log = shift || die ("no log passed");
    my $errors = shift || die ("no error passed");
    my $successes = shift || die ("no success file passed");

    # data structures for holding success/failures
    my %failed;    # keys=tests, values = options failed with
    my %succeeded; # keys=tests, values = options failed with

    # ---------- Parse Failures ------------
    # now, we should match all of the errors, to generate a report for each file
    # not for each file with each set of options
    my @parsed = $log =~ m/junit.framework.AssertionFailedError: (.*?)\((.*?) \)/gi;
    # for each failure line, split it up into the (test filename) and the options that were used 
    # to compile it, and save that in the associative hash %failed.
    while(@parsed) {
	my $test = shift(@parsed);
	my $options = shift(@parsed);
	$failed{$test} .= ("   " . $options . "\n");
    }


    # ---------- Parse Successes ------------
    @parsed = split("\n", $successes); # each line contains a success
    while(@parsed) {
	my $current_line = shift(@parsed);
	# split line into test and options
	my ($test, $options) = $current_line =~ m/(.*)\((.*)\)/g;
	$succeeded{$test} .= ("   " . $options . "\n");
    }

    # If we parsed errors, print out an error heading
    if (keys %failed) {
	print "\n\nFailures (see log file for details)\n";
	print "-------------------------------\n";
	my $current_key;
	foreach $current_key (sort keys %failed) {
	    print "$current_key \n" . $failed{$current_key};
	}
    }

    # If we parsed successes, print out an error heading
    if (keys %succeeded) {
	print "\n\nSuccesses (see log file for details)\n";
	print "-------------------------------\n";
	my $current_key;
	foreach $current_key (sort keys %succeeded) {
	    print "$current_key \n" . $succeeded{$current_key};
	}
    }
}

sub generate_performance_report {
    my $results_filename = shift || die("no result filename passed");

    print "Performance Numbers\n";
    print "--------------------------------\n";

    
    # hashmap that maps filenames and options to results
    my %results_hash;

    # open the file and parse it line by line
    open (FILE, "<$results_filename");

    while (<FILE>) {
	my $header = $_;   # first line
	my $data = <FILE>; # second line
	

	# split up the header into date, filename and compiler options
	my ($foo, $date, $filename, $options)      = split(/(.*?2002)\:(.*?)\((.*?)\)/, $header);
	# split up the data into hex_pc, dec_pc, and last_line_compared
	my ($bar, $hex_pc, $dec_pc, $last_line_compared) = split (/(.*?)\s(.*?)\s(.*?)\n/, $data);
	
	
	#print "date: $date-->filename: $filename-->options:$options\n";
	#print "hex: $hex_pc\ndecimal: $dec_pc\nlast line: $last_line_compared\n";

	# make up a key with filename and options
	my $key = $filename . "($options)";
	
	# add an entry to the results
	$results_hash{$key} .= "$dec_pc-->$date-->$last_line_compared-->";
    }
    
    # close the file
    close(FILE);


    # now, process out results: if we have more than one set of results 
    # for any key, print out some output.
    my $current_key;
    foreach $current_key (sort keys %results_hash) {
	print "$current_key:\n";
	print "  Date                         (Cycles) / (lines) = cycles/line\n"; 
	# each entry has pc, date, last line compared separated by -->
	# so split on --> and use results
	my @entries = split("-->", $results_hash{$current_key});
	while (@entries) {
	    my $current_pc = shift (@entries);
	    my $current_date = shift (@entries);
	    my $current_last_line = shift (@entries);

	    print("  $current_date ($current_pc) / ($current_last_line)" .
		  "   = " . $current_pc / $current_last_line . "\n");
	}
	
    }

}



# reads in a file and returns its contents as a scalar variable
# usage: read_file($filename)
sub read_file {
    my $filename = shift || die ("no filename passed to read_file\n");

    open(INFILE, "<$filename") || die ("could not open $filename");
    
    # save the old standard delimiter
    my $old_delim = local($/);
    # set the standad delimiter to undef so we can read
    # in the file in one go
    local($/) = undef; 

    # read in file contents
    my $file_contents = <INFILE>;
    close(INFILE);
    
    # restore the standard delimiter
    local($/) = $old_delim;

    return $file_contents;
}



# stupid perl syntax
1;

