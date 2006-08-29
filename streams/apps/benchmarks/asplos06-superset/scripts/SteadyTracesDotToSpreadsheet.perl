#!/usr/uns/bin/perl
#
###############################################################################
# Take as input a file where each line is the path and file name of a 
# steadyTraces.dot file.  Outputs a semicolon-separated-values spreadsheet 
# with columns for:
# benchmark -- the element of the path before /steadyTraces.dot w/o extension
# options -- the extension for the benchmark, after 'raw'
# slices -- the number of slices (number of occurrences of 'subgraph'
# total_nodes -- the total number of filters in all slices \d+[ label=...
#                - 2 * slices since each slice contains 1 splitter and 1 
#                joiner as well as filters.
# nodes_min -- minumum number of nodes in any slice. With current compiler is
#              1 if there are any FileReader's or FileWriter's
# nodes_mean -- average number of nodes per slice
# nodes_max  -- maximum number of nodes in any slice.
# nodes_stdev -- standard deviation.
# work_min -- minimum amount of static work estimate in any slice, from 
#             BN Work: \d+.  With current compiler, Work is
#             0 for any FileReader's or FileWriter's.  We exclude 0
#             when calculating information about work.
# work_mean -- average amount of work estimated per slice.
# work_max  -- maximum amount of work estimated per slice.
# work_stdev -- standard deviation (yes it currently includes slices with
#               0 work estimated).
# 
# find . -name "steadyTraces.dot" -exec echo `pwd`/'{}' \; >! tracefiles
# setenv localscripts $STREAMIT_HOME/apps/benchmarks/asplos06/scripts
# $localscripts/SteadyTracesDotToSpreadsheet.perl tracefiles > tracefileinfo.csv
###############################################################################

use warnings;
use strict;

require $ENV{STREAMIT_HOME} . "/misc/scripts/NumericArraySummary.perl";

sub workFromSteadyTracesDot {
    my $filenameandpath = shift;
    local @ARGV = ($filenameandpath);

my $insubgraph = 0;
my $subgraphCount = 0;
my $nodeCount = 0;
my $work;
my @nodes;
my @works;

# read in lines from ARGV and process
foreach (<>) {
    chomp;
    if (/^subgraph /) {
	$subgraphCount++;
	$insubgraph++;
	$nodeCount = 0;
	undef($work);
    } elsif (/\}/ && $insubgraph) {
	push(@nodes,$nodeCount - 2);
	push(@works,$work);
	$insubgraph = 0;
    } elsif (/BN Work: ([0-9]+)\"/) {
	$work = $1;
    } elsif (/^\s*[]0-9]+\[\s*label=/) {
        $nodeCount++;
    } 
}

my @nonzero_works = grep { $_ != 0 } @works;


my %node_info = numericArraySummary(@nodes);
my %work_info = numericArraySummary(@nonzero_works);


print "$subgraphCount ; ";
print "$node_info{\"sum\"} ; ";
print "$node_info{\"min\"} ; $node_info{\"mean\"} ; $node_info{\"max\"} ; $node_info{\"stdev\"} ;   ";
print "$work_info{\"min\"} ; $work_info{\"mean\"} ; $work_info{\"max\"} ; $work_info{\"stdev\"}\n";
}


# print header
print "benchmark ; options ; slices ; total_nodes ; nodes_min ; nodes_mean ; nodes_max ; nodes_stdev ; ";
print "work_min ; work_mean ; work_max ; work_stdev\n";

foreach (<>) {
    chomp;
    my $filenameandpath = $_;
    my ($fullfilename) = /.*\/([_A-Za-z0-9]+)\.raw(.*)\/steadyTraces\.dot/;
    next unless -s $filenameandpath;  # ignore 0-length files.
    my $benchmark = $1;
    my $options = $2;
    $benchmark = "??" unless defined($benchmark);
    $options = "??" unless defined($options);
    $options = "-space" if $options eq "";
    print "$benchmark ; $options ; ";
    workFromSteadyTracesDot($filenameandpath);
}

