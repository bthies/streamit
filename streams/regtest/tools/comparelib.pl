#!/usr/uns/bin/perl
#
# Library routines for the comparison scripts (eg the functionality common
# between them.
#
# $Id: comparelib.pl,v 1.4 2007-06-19 06:27:19 thies Exp $
#####################

use strict;

# for RELATIVE compare:  percent difference between expected output and actual output allowed
my $TOLERANCE_RELATIVE = .20;
# for ABSOLUTE compare:  percent difference between expected output and actual output allowed
my $TOLERANCE_ABSOLUTE = 1;
# if the difference is below this number we attribute it to round off error
# (needed for comparing 0 to 1.9E-19...)
my $ZERO = 1E-6;


########################### Subroutines ###########################

# Compares an expected value against an actual value produced by coder generated with the
# compiler. The main difference is that we aallow the values to be difference by a certain
# "Tolerance" percent. If the expected value is the same as the output value
# to within (expected value * tolerance) we don't flag an error. Otherwise we print some 
# error messages to stderr and bomb out of perl with an status of 1
#
# Usage: compare_values_relative($line_number, $expected_output, $actual_output);
sub compare_values_relative {
    # get the arguments into some local variables, for goodness sake
    my $current_line    = shift;
    my $expected_output = shift;
    my $actual_output   = shift;
    
    # if the expected output and the actual output are not the same, print a warning to std out
    if ($actual_output ne $expected_output) {
	# this addes a rediculous amount of stuff to the error log
	# print stderr "warning($current_line): mismatch between actual:$actual_output and expected:$expected_output\n";
    }
    
    # if the difference between expected and actual outputs are greater than
    # the tolerance * expected output, exit with 1
    my $difference = abs($actual_output - $expected_output);
    my $max_allowable_difference = abs($TOLERANCE_RELATIVE * $expected_output);
    if (($difference > $max_allowable_difference) and ($difference > $ZERO)) {
	print stderr "Comparison Mismatch\n";
	print stderr "line: $current_line\n";
	print stderr "actual output/expected output: $actual_output / $expected_output\n";
	
	# calculate some useful numbers for the error message (to determine if it was
	# an acutal output error of if the tolerance is just too low)
	my $pct_difference;
	if ($expected_output != 0) {
	    $pct_difference = ($difference / $expected_output) * 100;
	} else {
	    # otherwise, we get a 100 percent difference
	    $pct_difference = 1;
	}
	$pct_difference = int ($pct_difference * 100) / 100;
	print stderr "Difference/Percent Difference: $difference / $pct_difference\%\n";
	print stderr "Max Allowable Difference/Percent Difference: $max_allowable_difference\% / ";
	print stderr ($TOLERANCE_RELATIVE*100) . "\%\n";
	exit(1);
    }
}


# Compares an expected value against an actual value produced by coder generated with the
# compiler. The main difference is that we aallow the values to be difference by a certain
# ASBOLUTE AMOUNT. If the expected value is the same as the output value
# to within this difference, we don't flag an error. Otherwise we print some 
# error messages to stderr and bomb out of perl with an status of 1
#
# Usage: compare_values_absolute($line_number, $expected_output, $actual_output);
sub compare_values_absolute {
    # get the arguments into some local variables, for goodness sake
    my $current_line    = shift;
    my $expected_output = shift;
    my $actual_output   = shift;
    
    # if the expected output and the actual output are not the same, print a warning to std out
    if ($actual_output ne $expected_output) {
	# this addes a rediculous amount of stuff to the error log
	# print stderr "warning($current_line): mismatch between actual:$actual_output and expected:$expected_output\n";
    }
    
    # if the difference between expected and actual outputs are greater than
    # the tolerance * expected output, exit with 1
    my $difference = abs($actual_output - $expected_output);
    my $max_allowable_difference = $TOLERANCE_ABSOLUTE;
    if (($difference > $max_allowable_difference) and ($difference > $ZERO)) {
	print stderr "Comparison Mismatch\n";
	print stderr "line: $current_line\n";
	print stderr "actual output/expected output: $actual_output / $expected_output\n";
	
	# calculate some useful numbers for the error message (to determine if it was
	# an acutal output error of if the tolerance is just too low)
	my $pct_difference;
	if ($expected_output != 0) {
	    $pct_difference = ($difference / $expected_output) * 100;
	} else {
	    # otherwise, we get a 100 percent difference
	    $pct_difference = 1;
	}
	$pct_difference = int ($pct_difference * 100) / 100;
	print stderr "Difference: $difference\n";
	print stderr "Max Allowable Difference: $max_allowable_difference\n";
	exit(1);
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



1; # wacky perl syntax -- have to return a "true" value in imported libraries
