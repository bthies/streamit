#!/usr/local/bin/perl
# converts all of the eps pictures in the current directory into tiff images
# using the "convert" command

use strict;

my $raw_data = `ls images/*.eps`; # get file listing
my @files = split("\n", $raw_data); # split on newline


# iterate over all the files to convert them
my $current_file;
foreach $current_file (@files) {
    my @filename_pieces = split(/\./, $current_file); # split on period
    my $base_filename = shift(@filename_pieces);      # get the base part of the filename

    #convert the image
    print "converting $base_filename.eps to $base_filename.tiff\n";
    print `convert $base_filename.eps $base_filename.tiff\n`;
}
    
