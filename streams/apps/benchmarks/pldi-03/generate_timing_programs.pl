#!/usr/local/bin/perl
# script that generates programs to time for results of frequencyreplacement

use strict;
require "reaplib.pl";

# input is "postfix:options:filename"

my @input_lines = ("normal:--unroll 100000:FIRProgram",
		   "part:--linearpartition:FIRProgram",

		   );

my $OUTPUTDIR = "timing";

# make sure that we have the benchmarks ready to go
print `make benchmarks`;
print `mkdir $OUTPUTDIR`;

#loop on the tests
foreach (@input_lines) {
    my ($postfix, $options, $filename) = split(":");
    
    print "$filename-$postfix:";
    # copy the file
    print `cp $filename.java $OUTPUTDIR/$filename.java`;
    
    #java->c
    do_streamit_compile($OUTPUTDIR, $filename, $options);
    print `cp $OUTPUTDIR/$filename.c $OUTPUTDIR/$filename-$postfix.c`;
    # normal c->exe
    do_c_compile($OUTPUTDIR, "$filename-$postfix");

    # copy to a "np" = no print file
    print `cp $OUTPUTDIR/$filename-$postfix.c $OUTPUTDIR/$filename-$postfix-np.c`;
    # remove prints
    remove_prints($OUTPUTDIR, "$filename-$postfix-np");

    # noprint c->exe
    do_c_compile($OUTPUTDIR, "$filename-$postfix-np");

    # and we are done
    print "(done)\n";
}
