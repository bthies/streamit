#!/usr/local/bin/perl
##########
# This script simply makes the files for many different configurations 
# of the beamformer app.
##########
use strict;

my $i;
my $j;
for ($i=1; $i<=12; $i+=1) {
    for ($j=1; $j<=4; $j+=1) {
	make_beamformer($i,$j);
    }
}




#########
# subroutine to to create a beamformer with the specified number of 
# beams and channels. Resturns the filename of the created file.
# Usage:
# $new_filename = make_beamformer($channels, $beams);
#########
sub make_beamformer {
    my $channels = shift || die ("no channels passed to make_beamformer");
    my $beams    = shift || die ("no beams passed to make_beamformer");

    # read in the templated beamformer
    my $contents = read_file("CoarseSerializedBeamFormer.java");

    # replace set the number of channels and beams appropiately
    $contents =~ s/final int numChannels           = 12;/final int numChannels           = $channels ;/gi;
    $contents =~ s/final int numBeams              = 4/final int numBeams              = $beams/gi;

    # make the new filename
    my $new_filename = "BF$channels" . "_$beams";
    # replace CoarseSerializedBeamFormer with the new filename to stop java complaints.
    $contents =~ s/CoarseSerializedBeamFormer/$new_filename/gi;
    
    # write the contents out to disk
    write_file($contents, "$new_filename.java");

    # and return the new filename
    return $new_filename;
    
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

# writes the contents of the first scalar argument to the 
# filename in the second argument
# usage: write_file($data, $filename)
sub write_file {
    my $data = shift || die("No data passed to write_file");
    my $filename = shift || die("No filename passed to write_file");
    
    open(OUTFILE, ">$filename");
    print OUTFILE $data;
    close(OUTFILE);
}
