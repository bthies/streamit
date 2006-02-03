#! /usr/uns/bin/perl
#
# perl script to take the result of status2.xsl and determine the status of tests
#
# xsltproc -o foo.out --novalid status_summary.xsl /home/bits8/streamit/regtest/latest/streams/results.xml
# status_summary.pl foo.out > status_summary.txt
#
# The input file starts with a line "<?xml version="1.0"?>"
# The input file ends with some blank lines.
#
# All lines inbetween should be of the form
# testNameIncludingPeriods.compiler.testType RESULT
#   where 
#     testType is on of "compile", "run", or "verify"
#     RESULT is one of "PASS", "FAIL", or occasionally "ERROR"
#     (the other qmtest result "UNTESTED" is filtered out by status2.xsl)
#
#
# We want to get the status of a test --
# How far did the test get? compile? run? verify?
# and what was the final result.
#
# We expect the following possibilities
# verify PASS            compiled, ran, and output was verified.
# verify FAIL            compiled, ran, but output was incorrect
# run PASS               compiled and ran, but no data to verify against
# run FAIL               compiled but runtime error.
# run ERROR              similar -- often exceeded allowable run time.
# compile PASS           compiled and run in library as an indivisible process
#                         but no output to verify.
# compile FAIL           failed to compile
# compile ERROR          similar -- often exceeded allowable compile time.
#
# we lump FAIL and ERROR together in this report, and report 'ERROR' as 'FAIL'

use warnings;
use strict;


################################################################################
# static information for printing report.
################################################################################

my @summary_range = (0..4);


# @comparison_status
# strings to print for status change
# if results have improved or worsened.
my @comparison_status = ('compile-failed',
			 'run-failed',
			 'un-verified',
			 'un-verified',
			 'verify-failed',
			 'passed');


# %summary_order
# Map from contatenation of phase and outcome fields to 
# order in which results are printed.
# Numbers also used to sort for improvment / worsening between runs.
my %summary_order = 
    ('compileFAIL' => 0,
     'runFAIL' => 1,
     'verifyFAIL' => 2,
     'compilePASS' => 3,  # for compile & go such as 'strc -library'
     'runPASS' => 3,      # 3 considered a pass: no verification requested
     'verifyPASS' => 4);

# breakup of buckets for success / failure line
my @failure_indices = (0,1,2);
my @success_indices = (4,3);

# strings for success / failure line
my @succeed_fail_strings = (' compile, ',
			   ' execute, ',
			   ' verify',
			   ' not verified -- no output to compare to',
			   ' passed, ');

# headers for printing lists of tests
my @summary_headers = ('For the following benchmarks, COMPILATION failed:',
		       'For the following benchmarks, EXECUTION failed:',
		       'For the following benchmarks, VERIFICATION failed:',
		       'The following benchmarks executed, but can NOT be '
		       . 'VERIFIED because there is no output to compare to:',
		       'The following benchmarks PASSED:');

#################################################################################
# Main routine:  invoke the program with one file name to get breakdown on 
#   status of benchmarks.   Invoke with two filenames to get changess from second
#   file to first file and breakdosn of first file.
#################################################################################

MAIN: {
    # one or two input files.
    my ($f1, $f2) = @ARGV;

    my @results1 = get_benchmarks_status_array($f1);
    my @results2 = ();
    my @comparison_outputs = ();
    if ($f2) {
	@results2 = get_benchmarks_status_array($f2);
	@comparison_outputs = &format_comparison(\@results1, \@results2);
    }
    my @status_summary = ();
    my @status_details = ();
    &format_status(\@results1, \@status_summary, \@status_details);

    
    foreach (@status_summary, @comparison_outputs, @status_details) {
	print "$_\n";
    }

    
    exit;
}

#################################################################################
#
# Given: the name of a file containing lines with:    id phase result
# Returns: a list sorted on id that contains only lines for the result
# that is of interest -- the last PASS or FAIL in the sequence of 
# phases.  (Changes "ERROR" to "FAIL".)
# 
#################################################################################

sub get_benchmarks_status_array {
    my $filename = shift || die "Bad input.";
    my @benchmarkstatus;
    local @ARGV = ($filename);

    foreach (<>) {
	next unless /([a-z0-9_.]+)\.(compile|run|verify) (PASS|FAIL|ERROR)/;
	my $outcome = $3;
	$outcome = "FAIL" if $outcome eq "ERROR";
	my @testresult = ($1, $2, $outcome);
	push(@benchmarkstatus, \@testresult);
    }
#
# sort into order by testid, such that first occurrence for a test is the
# one we should report
#
    my @sortedstatus = sort(sort_benchmarkstatus @benchmarkstatus); 

    my $previd = "";
    my @lateststatus = grep {my $id = @$_[0]; 
			     my $r = ($previd ne $id); 
			     $previd = $id; 
			     $r}  @sortedstatus;

    return @lateststatus;
}


# sub print_id_phase_result_array {
#     foreach (@_) {
# 	my ($id, $phase, $result) = @$_;
# 	print "$id  $phase  $result\n";
#     }
# }

#
# sorting routine: by id, by phase in reverse order of execution within id.
#
sub sort_benchmarkstatus {
    my ($aid, $aphase, $aresult) = @$a;
    my ($bid, $bphase, $bresult) = @$b;
    my $compare = $aid cmp $bid;
    return $compare unless $compare == 0;
    return 0  if $aphase eq $bphase;
    return -1 if $aphase eq "verify";
    return 1  if $bphase eq "verify";
    return -1 if $aphase eq "run";
    return 1  if $bphase eq "run";
    return -1 if $aphase eq "compile";
    return 1  if $bphase eq "compile";
    die "Presumably impossible case in sort.";  
}

#############################################################################
#
# format a run's status
#
# Given: reference to run results array in first input,
# Returns: through summary array ref in second input and details array ref 
#  the summary and detailed status.
# There is no return value from this procedure.
#
#############################################################################

sub format_status {
    my ($results, $summary, $details) = @_;
    my @results = @$results;

    my @summary_buckets = ([], [], [], [], []);

    foreach (@results) {
	my ($id, $phase, $outcome) = @{$_};
	my $stat_num = $summary_order{$phase . $outcome};
	push @{$summary_buckets[$stat_num]}, $id;
    }
    #
    # Summary: total nombers of successes and failures
    #
    my $num_failures = 0;
    my $num_successes = 0;
    foreach (@failure_indices) {
	$num_failures += $#{$summary_buckets[$_]}+1;
    }
    foreach (@success_indices) {
	$num_successes += $#{$summary_buckets[$_]}+1;
    }
    my $failure_line = "";
    $failure_line .= sprintf ("%4d failures  (", $num_failures);
    foreach (@failure_indices) {
        $failure_line .= $#{$summary_buckets[$_]}+1 . $succeed_fail_strings[$_];
    }
    $failure_line .=  ")\n";
    push(@{$summary}, $failure_line);
    my $success_line;
    $success_line .= sprintf("%4d successes (", $num_successes);
    foreach (@success_indices) {
	 $success_line .= $#{$summary_buckets[$_]}+1 . $succeed_fail_strings[$_];
    }
    $success_line .= ")\n";
    push(@{$summary}, $success_line);

    #
    # now fill in array with details
    #

    foreach (@summary_range) {
	push (@{$details}, "\n$summary_headers[$_]\n");
	my $bucket = $summary_buckets[$_];
	foreach (@{$bucket}) {
	    push @{$details}, "  $_";
	}
    }
    

}

  
#############################################################################
#
# format a comparison between two test runs:
#
# first parameter is result array ref for current run.
# second parameter is result array ref for previous run.
#
# returns an array of strings to be output comparing the two runs
#############################################################################

sub format_comparison {
    my ($result1, $result2) = @_;
    my @outputs = ();

    # Partition result1, result2
    #
    # all of @in_1_only, @result1, @in_2_only, @result2, @in_both
    # are in lexicographical order

    my @result1 = @$result1;
    my @result2 = @$result2;

    my @in_1_only = ();
    my @in_2_only = ();
    my @worsened = ();
    my @improved = ();

    my $i1 = 0;
    my $i2 = 0;
    
    while ($i1 < @result1 && $i2 < @result2) {
	my ($id1, $phase1, $result1)  = @{$result1[$i1]};
	my ($id2, $phase2, $result2)  = @{$result2[$i2]};
	
	my $c = $id1 cmp $id2;
	if ($c < 0) {
	    push @in_1_only, $id1; $i1++;
	} elsif ($c > 0) {
	    push @in_2_only, $id2; $i2++;
	} else {
	    $i1++; $i2++;
	    my $disposition1 = $summary_order{$phase1 .  $result1};
	    my $disposition2 = $summary_order{$phase2 . $result2};
	    my $cc = ($disposition2 <=> $disposition1);
	    if ($cc < 0) {
		push @improved, "  $id1 ($comparison_status[$disposition2] -> $comparison_status[$disposition1])";
	    } elsif ($cc > 0) {
		push @worsened, "  $id1 ($comparison_status[$disposition2] -> $comparison_status[$disposition1])";
	    }
	} 
    }
    while ($i1 < @result1) { push @in_1_only, @{$result1[$i1++]}[0]; }
    while ($i2 < @result2) { push @in_2_only, @{$result2[$i2++]}[0]; }

    #
    # output added and removed test names
    #

    if (@in_1_only) {
	push @outputs, "\nNEW tests since last run:\n";
	foreach (@in_1_only) {
	    push @outputs, "  $_";
	}
    }

    if (@in_2_only) {
	push @outputs, "\nTests NOT RUN in this run, but in last run:\n";
	foreach (@in_2_only) {
	    push @outputs, "  $_";
	}
    }

    #
    # select tests that changed status between the runs and 
    # output tests thath have improves and tests that have worsened.
    #

    if (@improved) {
	push @outputs, "\nTests IMPROVED in this run over last run:\n";
	push @outputs, @improved;
    }

    if (@worsened) {
	push @outputs, "\nTests WORSENED in this run over last run:\n";
	push @outputs, @worsened;
    }
    return @outputs;
}
