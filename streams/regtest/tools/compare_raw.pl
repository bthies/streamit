#!/usr/local/bin/perl
# Script to parse the output of "make run" from the
# RAW btl simulator and compare it against
# an expected output file. The first command line argument
# is raw output, and second argument is expected output. 
#
# Returns with exit status 0 if the raw output and the expected output
# match up to the end of the shortest one (this is necessary because
# running a raw program an exact number of times is hard (because
# all you seem to be able to specify is the number of simulation cycles.)
#
# Prints out the cycle of the last btl cycle that produced output that was
# checked against expected results followed by a space followed by the 
# last line number of the expected results that was used for a comparison. 
# Example:
#   
#
# ex compare_raw.pl raw_output.txt expected_output.txt
# AAL 6/26/2002
# $Id: compare_raw.pl,v 1.1 2002-06-26 17:34:38 aalamb Exp $
#######

use strict;

# just kick it back to the old skool c style of doing things
main();





### Main Entry Point 
sub main {
    #read in the command line arguments
    my $raw_filename      = shift(@ARGV) || die (get_usage());
    my $expected_filename = shift(@ARGV) || die (get_usage());
    
    # read in the file contents
    my $raw_contents      = read_file($raw_filename);
    my $expected_contents = read_file($expected_filename);

    # split up the expected value based on newlines (so we can compare the output line by line);
    my @expected_items = split(/\n/, $expected_contents);

    # now, run a regexp pattern match on raw results
    # to extract the actual output from the simulator
    my @items = $raw_contents =~ m/\[(.+\:)\s(.+)]\:\s(.*)\n/g;
   
    # keep track of the current line number
    my $current_line = 1; 
    # keep track of the current pc
    my $current_pc;

    # while we still have both raw items and expected items to compare
    while (@items and @expected_items) {
	my $raw_number  = shift(@items); # I think it is a line number of the printf
	   $current_pc  = shift(@items); # pc where the output occured
	my $raw_output  = shift(@items); # what the program actually outputs
	
	my $expected_output = shift(@expected_items);
	#print ("line: $current_line, pc: $current_pc\n");
	#print ("output: $raw_output\nexpected: $expected_output\n");
	
	# if there is a difference between expected and actual, exit with 1
	if ($raw_output ne $expected_output) {
	    print "line: $current_line\n";
	    print "Raw output: $raw_output\n";
	    print "Expected output: $expected_output\n";
	    exit(1);
	}
	
	# update current line
	$current_line++;
    }


    # if we get here, it means that the two files were identical up to the shortest one
    $current_line--;
    print("$current_pc $current_line\n");
    exit(0); # all done
}







################################
# Subroutines
################################


# get a usage message
sub get_usage {
    return "usage: compare_raw.pl raw_output.txt expected_output.txt\n";
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
    
    
