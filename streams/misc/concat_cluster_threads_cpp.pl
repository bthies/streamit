#! /usr/bin/perl
#
# In fusion.cpp / master.cpp: remove "extern" from the begining of any line.
# In threadXXX.cpp: remove any line starting with extern.
# Keep set of #include statements: filter out redundant #include lines.
#
# write results out to combined_threads.cpp

use strict;
use warnings;
# used to declare new .h files at top of combined_threads.  currently
# not in use at all, since declarations needed were moved to fusion.cpp.
my %includes; # = ("message.h", 1);
my @top_includes; # = ("#include <message.h>");
my %move_to_top;

unless (@ARGV > 0 && ($ARGV[0] =~ "fusion.cpp" || $ARGV[0] =~ "master.cpp")) {
    print STDERR
	"Run as\nsingle_standalone_cpp.pl fusion.cpp threadNNNN.cpp ...\n";
    print STDERR
	" or as\nsingle_standalone_cpp.pl master.cpp threadNNNN.cpp ...\n";
    exit(1);
}

my $first_file = shift(@ARGV);

#####
# First pass: collect lines that must be moved up out of threadXXX.cpp files
#####

my @saved_ARGV = @ARGV;

while (<>) {
    # this data declration needs to be moved and then eliminated in pass 2
    if (/^(message \*__msg_stack_[0-9]+;\s+)$/) {
	$move_to_top{$1} = 1;
    }
    # these templates may occur in threadXXX.cpp files if strc is run
    # -cluster 10 -fusion
    # non-extern version of template should be moved up.
    if (/^extern (void __declare_sockets_[0-9]+\(\);\s+)$/
	|| /^extern (void __init_sockets_1\(void \(\*cs_fptr\)\(\)\);\s+)$/
	|| /^extern (void __flush_sockets_1\(\);\s+)$/
	|| /^extern (void __peek_sockets_1\(\);\s+)$/
	|| /^extern (void __init_thread_info_1\(thread_info \*\);\s+)$/
	|| /^extern (void __init_state_[0-9]+\(\);\s+)$/
	|| /^extern (void __push_[0-9]+_[0-9]+\([a-zA-Z0-9_\*\(\)\[\]]+\);\s+)$/  ) {
	$move_to_top{$1} = 1;
    }
}

@ARGV = @saved_ARGV;

#####
# second pass
#####

open (FUSION, $first_file)
    or die "Can not open $first_file file for input: $!";

#
# copy initial file removing initial "extern "
#

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

#
# put out moved lines and the includes that they need.
#

print COMBINED "\n// moved or inserted by concat_cluster_threads.pl\n";
for (@top_includes) {
    print COMBINED "$_\n";
} 

for (keys(%move_to_top)) {
    print COMBINED "$_";
}
print COMBINED "\n// end of moved or inserted by concat_cluster_threads.pl\n\n";

#
# concatenate to output removing redundancies.
#
while(<>) {
    $_ = proc_include($_);
    print COMBINED $_  unless /^\s*extern\s+(.*)$/;
}

#
# subroutine to remove redundant #include lines.
# and lines from move_to_top.
#
sub proc_include {
    my $line = shift;

    if ($line =~ /^\s*\#include\s+<?"?([_a-zA-Z0-9.\/]+\.h)/) { #"
	if ($includes{$1}) {
	    return "";
	} else {
	    $includes{$1} = 1;
	}
    }
    if ($move_to_top{$line}) {
	return "";
    }
    return $line;
}
