#!/usr/uns/bin/perl

###############################################################################
# Turn off printf's for timing test.
#
# use:
#   turnOffPrints foo.cpp
#
# result:
#    your old foo.cpp will be overwritten by a file that has all
#    printf statements commented out and replaced by calls to z___print
#    and a definition of z___print is put at the top of the file.
#
# The function z___print mught be simpler, but 
#
# With inspiration and some stolen code from Andrew Lamb
###############################################################################

use strict;
use warnings;

# replaces all occurences of printf with //printf in the specified file
sub remove_prints {
    my $pathAndName = $_[0] || die ("need a file name (with path)");
    open (my $fileHandle, "+< $pathAndName") || die "Cannot open $pathAndName $!";
    # read in the c file
    my $contents = read_file($fileHandle);
    # replace printf with //printf
    $contents =~ s/(\S\s+|;)(printf)/$1\/\/$2/g;
    # remove // TIMER_PRINT_CODE:
    $contents =~ s/\/\/ TIMER_PRINT_CODE: //g;
    # write the changes back to disk
    seek $fileHandle, 0, 0;
    truncate $fileHandle, 0;
    print $fileHandle 'volatile int __print_sink__;', "\n";
    write_file($contents, $fileHandle);
    close($fileHandle);
}



# writes the contents of the first scalar argument to the 
# filename in the second argument
# usage: write_file($data, $fileHandle)
sub write_file {
    my $data = shift || die("No data passed to write_file");
    my $fileHandle = shift || die("No filename passed to write_file");
    
    print $fileHandle $data;
}


# reads in a file and returns its contents as a scalar variable
# usage: read_file($fileHandle) where handle refers to an open file.
sub read_file {
    my $fileHandle = shift || die ("no handle passed to read_file\n");

    # save the old standard delimiter
    my $old_delim = local($/);
    # set the standad delimiter to undef so we can read
    # in the file in one go
    local($/) = undef; 

    # read in file contents
    my $file_contents = <$fileHandle>;
    # restore the standard delimiter
    local($/) = $old_delim;

    return $file_contents;
}

MAIN: {
    remove_prints(@ARGV);
}

1;
