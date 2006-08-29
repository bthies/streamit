#!/usr/uns/bin/perl
#
###############################################################################
# Take the results.out files for raw and for spacetime and put out lines for
# a spreadsheet (in semicolon-separated-value format).
#
# Create columns for:
#  (1) benchmark name
#  (2) benchmark options
#  (3) throughput, defined as number of cycles per data item output
#  (4) utilization, defined as used instruction issues / available issues.
#  (%) MFLOPs
#
# The input to this script is a list of result.out files for which to create 
# rows in the spreadsheet.  The files are expected to be in subdirectories
# of the CWD with names of the form  ... .raw...
# The lowest subdirectory name up to '.raw' is output as the benchmark name.
# the characters in the directory name after '.raw' are output as the
# options, unless there are no such characters, in which case the options
# field is set to '-space'.
#
# To use, do something like:
#   
# find . -name "results.out" -exec echo `pwd`/'{}' \; > resultfiles
# $STREAMIT_HOME/apps/benchmarks/asplos06/scripts/ResultsToSpreadsheet.perl \
#  resultfiles > results.csv
#
# You can then load results.csv into a spreadsheet, or sort it and look at 
# the semicolon-separated format.  If the latter, you may want to restore
# the column headers line to the top of the file from wherever sort left it.
# (we could do something with head, wc, and tail,  or with grep to remove
#  and replce the header line, but not worth the effort right now.)
#
# Notes:
# (1) The results.out files for -raw  and -raw -spacetime are substantially
#  different.
# (2) Mike Gordon says that the utilization numbers for -raw versus 
#  -raw -spacetime do not allow a fair comparison, since there is extra
# code needed for -raw that is not needed for -raw -spacetime...
# 
###############################################################################

use warnings;
use strict;
use Getopt::Std;

my $spacetime = 1;

my %options=();
getopts("s", \%options) or die("ResultsOutToSpreadSheet [-s]");

$spacetime = 0 if defined $options{s};

# print header same for each backend
print "benchmark;options;throughput;utilization;MFLOPS;filters;slices;BufSizeBytes;correct\n";

foreach (<>) {
  chomp;
  my $filenameandpath = $_;
  my ($fullfilename) = /.*\/([_A-Za-z0-9]+)\.raw(.*)\/results.out/;
  next unless -s $filenameandpath; # ignore 0-length files.
  my $benchmark = $1;
  my $options = $2;
  my @mflops = ();
  my $tiles = "";
  my $used_tiles = "";
  my $filters;
  my $slices;
  my $throughput; 
  my $cycles = "";
  my $outputs = "";
  my $instrs_issued;
  my $max_instrs_issued;
  my $utilization = "";
  my $mflops = "";
  my $buf_size;
  my $correct = 0;
  if ($spacetime == 1) {
    my $results;
    open (RESULTS, "< $filenameandpath") or next;
    while (<RESULTS>) {
      chomp;
      #we are looking at a spacetime file
      if (/^([0-9]+);([0-9]+);([0-9]+);([0-9]+);([0-9]+);([0-9]+);([0-9]+);([0-9]+);([0-9]+);([0-9]+)$/) {
	#tiles;assigned;num_filters;num_slices;throughput;work_cycles;total_cycles;mflops;buf_size
	$tiles = $1;
	$used_tiles = $2;
	$filters = $3;
	$slices = $4;	
	$cycles = $5;
        $outputs = $6;
	$instrs_issued = $7;
	$max_instrs_issued = $8;
	$mflops = $9;
        $buf_size = $10;
        $throughput = $cycles / $outputs;
	if (defined($instrs_issued) && defined($max_instrs_issued )) {
	  $utilization = $instrs_issued / $max_instrs_issued;
	}
      } elsif (/PASSED/) {
	$correct = 1;
      }
    }				  
    $results =  "$benchmark;$options;$throughput;$utilization;$mflops;$filters;$slices;$buf_size;$correct\n";
    #make options sort
    $results =~ s/steadymult(\d)(\D)/steadymult00$1$2/;
    $results =~ s/steadymult(\d)(\d)(\D)/steadymult0$1$2$3/;
    $results =~ s/slicethresh(\d)(\D)/slicethresh00$1$2/;
    $results =~ s/slicethresh(\d)(\d)(\D)/slicethresh0$1$2$3/;
    print $results;
    close(RESULTS);
  }
  else {
    my $data = `(tail -1 "$filenameandpath")`;
    chomp($data);
    #print "$data\n";
    #tiles;used;avg_cycles/steady;XX;XX;XX;outputs_per_steady;??;MFLOPS;instr_issued;XX;XX;XX%;XX;XX;max_instrs_issued
    $data =~/^([0-9]*);([0-9]*);([0-9]*);[0-9]*;[0-9]*;[0-9]*;([0-9]*);[0-9]*;([0-9]*);([0-9]*);[0-9]*;[0-9]*;[0-9]*%;[0-9]*;[0-9]*;([0-9]*)$/;
    my $tiles = $1;
    my $used_tiles = $2;
    my $cyc_per_steady = $3;
    my $outputs_per_steady = $4;
    my $mflops = $5;
    my $instrs_issued = $6;
    my $max_instrs_issued = $7;
    my $throughput = "";
    if (defined($cyc_per_steady) && defined($outputs_per_steady)) {
      $throughput = $cyc_per_steady / $outputs_per_steady;
    }
    my $utilization = "";
    if (defined($instrs_issued) && defined($max_instrs_issued )) {
      $utilization = $instrs_issued / $max_instrs_issued;
    }
    $mflops = "" unless defined($mflops);
    print "$benchmark;$options;$throughput;$utilization;$mflops;$used_tiles;0;NA\n";
  }
}


