#!/usr/local/bin/perl

# this script inputs the data that is output from reap_fir.pl script and
# makes multiple columns of data intended for doing a 3d-mesh surface plot
#a la the theoretical one in the paper

use strict;

# eat first line
<STDIN>;

# global hash for storing rows
my %row_hash;
# global hash for storing headings
my %heading_hash;

while (<STDIN>) {
    chomp;
    # parse the input line by splitting on tabs
    my ($program, $fir_size, $target_size, $actual_size, 
	$normal_flops, $normal_fadds, $normal_fmuls, $normal_outputs,
	$freq_flops, $freq_fadds, $freq_fmuls, $freq_outputs) = split("\t");
    
    # calculate the multiplication savings
    my $normal_fmuls_per_output = $normal_fmuls/$normal_outputs;
    my $freq_fmuls_per_output   = $freq_fmuls  /$freq_outputs;
    my $savings = $freq_fmuls_per_output / $normal_fmuls_per_output;

    # update the data hashes
    $row_hash{$fir_size}     .= "$savings\t";
    $heading_hash{$fir_size} .= "$target_size\t";
}

my @fir_keys = sort keys %row_hash;
my $heading = $heading_hash{pop(@fir_keys)};

print "FIR size/target size\n";
print "\t$heading\n";


my $current_fir_size;
foreach $current_fir_size (sort keys %row_hash) {
    print $current_fir_size . "\t" . $row_hash{$current_fir_size} . "\n";
}
