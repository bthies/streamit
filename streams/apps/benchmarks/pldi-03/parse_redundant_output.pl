#!/usr/local/bin/perl

# parse the output of a linear analysis run, extracting the linear redundancy 
# part.
#
# usage: parse_linear_output.pl output_trace_from_compiler.txt

use strict;

main();


sub main {
    my $filename = shift(@ARGV) || die("usage: parse_redundant_output.pl filename");

    # read in the contents to a scalar
    my $output_contents = read_file($filename);

    my @lines = $output_contents =~ m/Linear Redundancy Analysis:\n(.*)end\.\n/sig;

    foreach (@lines) {
	print "found line:\n$_\n";
    }




}


# prints out key\nvalue pairs for the contents of the hash
sub print_hash_ref {
    my $hashref = shift || die ("no hash ref passed to print_hash_ref");
    my $current_key;
    foreach $current_key (sort keys %$hashref) {
	print "$current_key\n";
	print $$hashref{$current_key} . "\n";
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

