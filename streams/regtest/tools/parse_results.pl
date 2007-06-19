#!/usr/uns/bin/perl

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
# $Id: parse_results.pl,v 1.8 2007-06-19 06:27:19 thies Exp $

use strict;

# entry point
main();


sub main {
    my $log_filename     = shift(@ARGV) || die ("No log filename supplied\n");
    my $error_filename   = shift(@ARGV) || die ("No error filename supplied\n");
    my $success_filename   = shift(@ARGV) || die ("No success filename supplied\n");

    # read in the contents of the log, error, and success files
    my $log_contents     = read_file($log_filename);
    my $error_contents   = read_file($error_filename); 
    my $success_contents = read_file($success_filename);

    # generate an executive summary of the results of the regression tests
    generate_executive_summary($log_contents);
    # generate a failure report
    generate_failure_report($log_contents, $error_contents, $success_contents);

    print "\n\n";

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
    my $log = shift;
    my $errors = shift;
    my $successes = shift;

    # data structures for holding success/failures
    my %failed;    # keys=tests, values = options failed with
    my %succeeded; # keys=tests, values = options succeeded with

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
	print ("\n\n" . count_total_options(\%failed) . 
	       " Failures (see log file for details)\n");
	print "-------------------------------\n";
	my $current_key;
	foreach $current_key (sort keys %failed) {
	    print "$current_key \n" . $failed{$current_key};
	}
    }

    # If we parsed successes, print out an error heading
    if (keys %succeeded) {
	print ("\n\n" . count_total_options(\%succeeded) . 
	       " Successes (see log file for details)\n");
	print "-------------------------------\n";
	my $current_key;
	foreach $current_key (sort keys %succeeded) {
	    print "$current_key \n" . $succeeded{$current_key};
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


# counts the number of compiler options there are in the success or failure
# hashes. These hashes each have "Compile|Execute|Verify filename" as keys
# and multiple lines of <sp><sp><sp>options
# this sub go through and counts all of those options for all keys, so we can report the
# number of successes and the number of failures that we have
sub count_total_options {
    my $hashref = shift || die ("No hash ref passed to count_total_options");
    
    # basic idea: for each key, we count the number of newlines
    my $total_options = 0;
    my $current_key;
    foreach $current_key (keys %$hashref) {
	my @items = split("\n", $$hashref{$current_key});
	$total_options += @items;
    }
    return $total_options;
}
    




# stupid perl syntax
1;

