#!/usr/local/bin/perl

# parse the output of compiler run with linear analysis and verboose mode turned on,
# and extract the matrix/vector pairs that correspond to each linear filter. 
#
# usage: parse_linear_output.pl output_trace_from_compiler.txt

use strict;

main();

sub main {
    my $filename = shift(@ARGV) || die("usage: parse_linear_output.pl filename");

    # read in the contents to a scalar
    my $output_contents = read_file($filename);

    my @filter_contents;
    my @pipeline_contents;
    my @splitjoin_contents;
    # do a pattern match to extract the matrix/vector pairs for filters
    @filter_contents = $output_contents =~ m/Linear filter found: .*?name=(.*?)\d*?_\d*?\n-->Matrix:\n(.*?)-->Constant Vector:\n(.*?]]\n)/sig;
    @pipeline_contents = $output_contents =~ m/Linear pipeline found: .*?name=(.*?)\d*?_\d*?\n-->Matrix:\n(.*?)-->Constant Vector:\n(.*?]]\n)/sig;
    @splitjoin_contents = $output_contents =~ m/Linear splitjoin found: .*?name=(.*?)\d*?_\d*?\n-->Matrix:\n(.*?)-->Constant Vector:\n(.*?]]\n)/sig;

    # combine the set of contents together
    my @contents = (@filter_contents, @pipeline_contents, @splitjoin_contents);


    my %data;
    while(@contents) {
	my $name = shift(@contents);
#my $foo= shift(@contents);
	my $matrix=shift(@contents); 
        chomp($matrix);
	my $vector = shift(@contents);
	chomp($vector);
	$data{"$name.matrix"} = $matrix;
	$data{"$name.vector"} = $vector;
    }

    print_hash_ref(\%data);

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

