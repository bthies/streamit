#!/usr/local/bin/perl


# does a little parsing magic for matlab debugging

use strict;

my @input_data;

while (<STDIN>) {
    my ($current_input) = m/:(.*)\n/gi;

    chomp($current_input);
    push(@input_data, $current_input);
}


# generate some matlab code
print "x = [";
print join(" ", @input_data);
print "]'\n";

