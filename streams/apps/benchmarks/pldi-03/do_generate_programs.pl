#!/usr/local/bin/perl
# script that generates programs to time for results of frequencyreplacement

use strict;
require "reaplib.pl";
my $OUTPUTDIR = "timing";

if (not @ARGV) {
    print "usage: do_timing_programs.pl inputscript\n";
    print "  input script has on separate lines: postfix:options:filename\n";
    die();
}
# input is "postfix:options:filename"
# select the tests to run
my @input_lines = split("\n", read_file(shift(@ARGV)));

# make sure that we have the benchmarks ready to go
print `make benchmarks`;
print `mkdir -p $OUTPUTDIR`;

#loop on the tests
foreach (@input_lines) {
    # ignore blank lines...
    if (not $_) {next;}
    my ($postfix, $options, $filename) = split(":");
    
    # if the file already exists, then skip
    if (-e "$OUTPUTDIR/$filename-$postfix.c") {
	print "$filename-$postfix.c: already exists\n";
	next;
    }

    print "$filename-$postfix:";
    # copy the file
    print `cp $filename.java $OUTPUTDIR/$filename.java`;
    
    #java->c
    do_streamit_compile($OUTPUTDIR, $filename, $options);
    print `cp $OUTPUTDIR/$filename.c $OUTPUTDIR/$filename-$postfix.c`;
    
    # save the generated dot files (before, after, and linear)
    if (-e "$OUTPUTDIR/before.dot") {
	`mv $OUTPUTDIR/before.dot $OUTPUTDIR/$filename-$postfix-before.dot`;
    }
    if (-e "$OUTPUTDIR/after.dot") {
	`mv $OUTPUTDIR/after.dot $OUTPUTDIR/$filename-$postfix-after.dot`;
    }
    if (-e "$OUTPUTDIR/linear.dot") {
	`mv $OUTPUTDIR/linear.dot $OUTPUTDIR/$filename-$postfix-linear.dot`;
    }
    if (-e "$OUTPUTDIR/linear-simple.dot") {
	`mv $OUTPUTDIR/linear-simple.dot $OUTPUTDIR/$filename-$postfix-linear-simple.dot`;
    } 
    if (-e "$OUTPUTDIR/partitions.dot") {
	`mv $OUTPUTDIR/partitions.dot $OUTPUTDIR/$filename-$postfix-partitions.dot`;
    }

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
