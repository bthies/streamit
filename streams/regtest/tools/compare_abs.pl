#!/usr/uns/bin/perl
#
# This is like compare_uni, but uses an absolute tolerance (default 1)
# rather than a percent tolerance.
#
# script to replace cmp used to compare the values of two 
# files, line by line. cmp is not used because we allow the
# two values to be difference by a small (but non zero)
# tolerance.
#
# Returns with exit status 0 if the contents  output and the expected output
# match up to the end of the shortest one. Returns with exit status 1 if the 
# two files are not the same. 
#
# Usage: compare_abs.pl actual_output.txt expected_output.txt
#
# AAL 7/22/2002
# $Id: compare_abs.pl,v 1.1 2007-06-19 06:27:19 thies Exp $
#######

use strict;
use lib ($ENV{'STREAMIT_HOME'} . '/regtest/tools');
require "comparelib.pl";

# c-style
main();

##### Main Entry Point #####
sub main {
    #read in the command line arguments
    my $actual_filename   = shift(@ARGV) || die (get_usage());
    my $expected_filename = shift(@ARGV) || die (get_usage());
    
    # read in the file contents -- actual output and expected file
    my $actual_contents   = read_file($actual_filename);
    my $expected_contents = read_file($expected_filename);

    # split up the values based on newlines (so we can compare the output line by line);
    my @actual_items   = split(/\n/, $actual_contents);
    my @expected_items = split(/\n/, $expected_contents);

    # keep track of the current line number
    my $current_line = 1; 

    # while we still have both raw items and expected items to compare
    while (@actual_items and @expected_items) {
	# pop off the front two items from the outputs
	my $actual_output   = shift(@actual_items); # what the program actually outputs
	my $expected_output = shift(@expected_items);


	# compare the value with the comparelib routine
	# this will exit with an error message if the value is not within tolerance.
	compare_values_absolute($current_line, $expected_output, $actual_output);
	
	# update current line
	$current_line++;
    }

    exit(0); # all done
}

################################
# Subroutines
################################


# get a usage message
sub get_usage {
    return "usage: compare_abs.pl actual_output.txt expected_output.txt\n";
}
