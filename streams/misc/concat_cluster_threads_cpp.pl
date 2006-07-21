#! /usr/uns/bin/perl
#
# In fusion.cpp / master.cpp: remove "extern" from the begining of any line.
# In threadXXX.cpp: remove any line starting with extern.
# Keep set of #include statements: filter out redundant #include lines.
#
# write results out to combined_threads.cpp

use strict;
use warnings;
my %includes;

unless (@ARGV > 0 && ($ARGV[0] =~ "fusion.cpp" || $ARGV[0] =~ "master.cpp")) {
    print STDERR
	"Run as\nsingle_standalone_cpp.pl fusion.cpp threadNNNN.cpp ...\n";
    print STDERR
	" or as\nsingle_standalone_cpp.pl master.cpp threadNNNN.cpp ...\n";
    exit(1);
}

my $first_file = shift(@ARGV);

open (FUSION, $first_file)
    or die "Can not open $first_file file for input: $!";

open (COMBINED, "> combined_threads.cpp") 
    or die "Can not open combined_threads.cpp: $!\n";
while (<FUSION>) {
    $_ = proc_include($_);
    if (/^\s*extern\s+(.*)$/) {
	print COMBINED "$1\n";
    } else {
	print COMBINED $_;
    } 
}

while(<>) {
    $_ = proc_include($_);
    print COMBINED $_  unless /^\s*extern\s+(.*)$/;
}


sub proc_include {
    my $line = shift;

    if ($line =~ /^\s*\#include\s+<?"?([_a-zA-Z0-9.\/]+\.h)/) { #"
	if ($includes{$1}) {
	    return "";
	} else {
	    $includes{$1} = 1;
	}
    }
    return $line;
}
