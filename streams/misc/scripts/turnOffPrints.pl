#!/usr/uns/bin/perl

###############################################################################
# Turn off printf's for timing test.
#
# use:
#   turnOffPrints foo.cpp
#
# result:
#    your old foo.cpp will be overwritten by a file that has all
#    printf statements commented out and replaced by writes to 
#    volatile variable __print_sink__.  
#    __print_sink__ is defined to be extern unless that file name being
#   processed is str.c, combined_threads.cpp, fusion.cpp, or global.cpp
#
# works with help of the print routine in at.dms.kjc.common.ToCCommon which
# flags the prints for easy removal.
#
#
# For use in the current regression test directory:
# find . -name "cluster_fusion_standalone.qms" | xargs -iDIR find DIR -name "*.cpp" -exec $STREAMIT_HOME/misc/scripts/turnOffPrints.pl {} \;
# similarly for other backends crating multiple .c or .cpp files.
# rstream can be recognized by just searching for "str.c"
#
# With inspiration and some stolen code from Andrew Lamb
###############################################################################

use strict;
use warnings;

# replaces all occurences of printf with //printf in the specified file
sub remove_prints {
    my $pathAndName = $_[0] || die ("need a file name (with path)");
    print "$pathAndName\n";
##########
# for in-place update without backup.
#    open (my $fileHandle, "+< $pathAndName") || die "Cannot open $pathAndName $!";
##########
##########
# for backing up old file.
    rename ($pathAndName, $pathAndName . ".bak") || die "Cannot move $pathAndName $!";
    open (my $fileHandle, "< $pathAndName.bak") || die "Cannot open $pathAndName .bak $!";
##########
    # read in the c file
    my $contents = read_file($fileHandle);
    # replace printf with //printf
    $contents =~ s/(\S\s+|;)(printf)/$1\/\/$2/g;
    # remove // TIMER_PRINT_CODE:
    $contents =~ s/\/\/ TIMER_PRINT_CODE: //g;
    # write the changes back to disk
##########
# for in-place update without backup.
#    seek $fileHandle, 0, 0;
#    truncate $fileHandle, 0;
##########
##########
# for backing up old file.
    close($fileHandle);
    open ($fileHandle, "> $pathAndName") || die "Cannot open $pathAndName $!";
##########
    my @cfilename = split (/\//, $pathAndName);
    my $cfilename = pop(@cfilename);
    if ($cfilename eq "str.c" || $cfilename eq "master.cpp" || $cfilename eq "fusion.cpp" || $cfilename eq "combined_threads.cpp") {
	print $fileHandle 'volatile int __print_sink__;', "\n";
    } else {
	print $fileHandle 'extern volatile int __print_sink__;', "\n";
    }
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
