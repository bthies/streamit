#!/usr/local/bin/perl
# Script to parse the output of "make run" from the
# RAW btl simulator. Takes the first command line argument
# as input, and prints output to stdout.
#
# ex parse_raw.pl output.txt
# AAL

use strict;

# rad first command line arg
my $filename = shift || die ("usage: parse_raw.pl filename\n");

# read input file into a local scalar
open(INFILE, "<$filename");
# set the end of file delimiter to 
# undef to get the contents of the file
local($/) = undef; 
my $file_contents = <INFILE>;
close(INFILE);

# now, run a regexp pattern match, and output the results
my @items = ($file_contents =~ m/\[.+\:.+]\:\s(.*)\n/g);
print "found " . @items . " items";
my $item;
foreach $item (@items) {
    print "$item\n";
}
