#! /usr/uns/bin/perl
###############################################################################
# To copy from files on command line to stdout replacing fileWriters with 
# prints.
#
# This script turns out to be useful for number gathering (the -N switch) with
# the original raw compiler, in which the -N switch only works for prints, not 
# for file writes.
#
# At some point I should write a script to revert the effect of this script...
###############################################################################

use strict;
use warnings;
 
foreach (<>) {
    chomp();
    # this line performs the replacing
    # if you want to use a perl 1-liner:
    #  perl -i -p -e 'TEXT_OF_NEXT_LINE'
    s/^\s*add\s+FileWriter<([a-z]*)>.*$/add $1->void filter{ work pop 1 { println(pop()); } }/;
    print "$_\n";
}

###############################################################################
# To process all .str files in the current directory subtree:
#
# find . -name "*.str" -exec echo `pwd`/{} \; | $STREAMIT_HOME/misc/scripts/FileWriterToPrintln.perl 
#
# Or try the in-place command:
# find . -name "*.str" -exec perl -i -p -e 's/^\s*add\s+FileWriter<([a-z]*)>.*$/add $1->void filter{ work pop 1 { println(pop()); } }/;' {} \; -print
#
###############################################################################
