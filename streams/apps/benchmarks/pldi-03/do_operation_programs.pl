#!/usr/local/bin/perl
# script that generates times the execution of programs and counts the output
# assumes the existence of programs generates by generate_timing_programs.pl

# Note that files with the postfix of postfix-np merely have had their
# printf's commented out


use strict;
require "reaplib.pl";
my $OUTPUTDIR = "timing";

# input is "postfix:options:filename"

if (not @ARGV) {
    print "usage: do_operation_programs.pl inputscript\n";
    print "  input script has on separate lines: postfix:options:filename\n";
    die();
}

# select the tests to run
my @input_lines = split("\n", read_file(shift(@ARGV)));

# place to put the results line
my @result_lines = ("Name\tOptions\tflops\tfadds\tfmuls\toutputs");

#loop on the tests
foreach (@input_lines) {
    if (not $_) {next;} #ignore blank lines
    my ($postfix, $options, $filename) = split(":");
    
    # if the files don't exist, then signal an error
    if (not (-e "$OUTPUTDIR/$filename-$postfix.exe")) {
	print "$filename-$postfix.exe doesn't exist\n";
	push(@result_lines, "$filename-$postfix\t$options\t0\t0\t0\t0");
	next;
    }
    if (not (-e "$OUTPUTDIR/$filename-$postfix-np.exe")) {
	print "$filename-$postfix-np.exe doesn't exist\n";
	push(@result_lines, "$filename-$postfix\t$options\t0\t0\t0\t0");
	next;
    }

    # for now, if we compiled with the atlas option, don't get op counts
    # because it breaks dynamo rio
    if ($options =~ m/atlas/gi) {
	print "atlas not supported by dynamorio yet:$options\n";
	push(@result_lines, "$filename-$postfix\t$options\t0\t0\t0\t0");
	next;
    }
    
    print "$filename-$postfix:";
    # run the program for the specified number of iterations until we find a number of iterations
    # around a second
    my $iters = 1;
    my $flag = 1; # 0 is true
    while ($flag == 1) {
	my $current_time = time_execution($OUTPUTDIR, "$filename-$postfix", $iters);
	if ($current_time > 1) {
	    # set the flag that we are done
	    $flag = 0;
	} else {
	    # double the target iterations
	    $iters *= 2;
	}
    }

    print "\nRunning dynamo for $iters iterations:";
    # run the dynamo rio test and get back the results
    my $report = run_rio($OUTPUTDIR, "$filename-$postfix", $iters);
    
    # extract the flops, fadds and fmul count from the report
    my ($flops) =  $report =~ m/saw (.*) flops/;
    my ($fadds) =  $report =~ m/saw (.*) fadds/;
    my ($fmuls) =  $report =~ m/saw (.*) fmuls/;

    # figure out how many outputs there are
    my $overall_outputs = get_output_count($OUTPUTDIR, "$filename-$postfix", $iters);
    
    # add a result line
    push(@result_lines, "$filename-$postfix\t$options\t$flops\t$fadds\t$fmuls\t$overall_outputs");

    print "done.\n";
}

# write the results to a tsv file
save_tsv("operationResults.tsv", "operations results", @result_lines);
	
