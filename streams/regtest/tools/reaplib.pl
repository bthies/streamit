#!/usr/local/bin/perl
# library routines for reaping performance data from
# the streamit compiler.
# $Id: reaplib.pl,v 1.2 2002-07-12 20:21:39 aalamb Exp $

use strict;


# Parse the results that came from the simulator into the file
# filename is the basefile name where all of the results are written
# init_output_count is the number of outputs created by initiaization (this
#   many outputs are ignored).
# ss_outout_count is the number of outputs created by a steady state iteration (used
#   to create the cycles/ss iter numbers).
# usage: parse_results($filename, $btl_output, $init_output_count, $ss_output_count)
#
# returns a string of the following format:
# "total cycles until the end of the first steady state cycly:average cycles per steady state iteration"
sub parse_results {
    my $parsed_output_filename = shift || die("No output filename passed to parse_results");
    my $sim_output = shift || die("No btl output passed to parse_results");
    my $init_output_count = shift ;#|| die("No initializaton count passed to parse_results");
    my $ss_output_count = shift || die("No steady state output count passed to parse_results");

    # now, run a regexp pattern match on the raw output
    # to extract the cycle counts and the actual output from the simulator
    # (how is that for an awful regular expression?)
    my @items = $sim_output =~ m/\[(.+\:)\s(.+)]\:\s(.*)\n/g;

    # now, create a scalar that contains the data we are going to write
    my $cycle_vs_item        = "cycle\toutput item\n";
    my $cycles_per_output     = "output number\tcycles\n";
    my $ss_cycles_per_output = "ss iteration number\tcycles\n";

    my $item_count = 0;
    my $output_count = 0;     #keep track of how many outputs we have seen since last steady state iter
    my $ss_iteration_count = 0; # how many steady state iterations we have seen

    my $last_base10_cycles = 0;
    my $last_base10_ss_cycles = 0;
    while(@items) {
	my $raw_number = shift(@items); # this is the first "10:" number
	my $cycle = shift(@items);
	my $output = shift(@items);

	# conver the cycle to base 10 (from hex)
	my $base10_cycles = hex($cycle);

	# keep numbers for cycles/item graph data
	$item_count++;
	$cycle_vs_item .= "$base10_cycles\t$item_count\n";
	
	# if we are still in init, ignore data for ss counts below
	if ($item_count < $init_output_count) {
	    next;
	}
       
	
	# figure out the number of cycles that it has taken to produce the current output
	my $current_cycles_per_output = $base10_cycles - $last_base10_cycles;
	$cycles_per_output .= "$item_count\t$current_cycles_per_output\n";

	# update the output count since the last iteration
	$output_count++;
	# if we are done with an iteration, caculate the number of cycles it took per iteration
	if ($output_count == $ss_output_count) {
	    $output_count = 0;
	    $ss_iteration_count++; # increment the number of steady-state iterations we have seen
	    my $current_ss_cycles_per_output = $base10_cycles - $last_base10_ss_cycles;
	    $ss_cycles_per_output .= "$ss_iteration_count\t$current_ss_cycles_per_output\n";
	    #update the last cycle time counter
	    $last_base10_ss_cycles = $base10_cycles;
	}
	
	#update the previous cycles for next iteration
	$last_base10_cycles = $base10_cycles;	
    }

    # write the data files to disk
    write_file($cycle_vs_item, "$parsed_output_filename.cycle-vs-item");
    write_file($cycles_per_output, "$parsed_output_filename.cycles-per-output");
    write_file($ss_cycles_per_output, "$parsed_output_filename.cycles-per-ss-iteration");

    # now we nee dto do a little magic: figure out the "average number of cycles per steady state"
    # which we will do by reparsing the data we have been gathering and averaging everything but the first
    @items = split("\n", $ss_cycles_per_output);
    # chop off the first item (the header) 
    shift(@items);
    # take the first entry in lines to be the time it takes to finish the first steady state cycle
    my ($foo, $first_ss_iter_cycle_count) = split("\t", shift(@items)); 
    # now, make an average steady state cycle time
    my $running_sum = 0;
    my $total_ss_iterations = 0;
    foreach(@items) {
	# each line is cycles\tIteration
	my ($current_ss_iter, $current_cycles) = split("\t");
	$running_sum += $current_cycles;
	$total_ss_iterations++;
    }
    # assemble return string
    my $average_cycles_per_ss = $running_sum / $total_ss_iterations;
    return "$first_ss_iter_cycle_count:$average_cycles_per_ss";
}


# writes an executive summary of the data (eg an entry in the asplos table)
# usage: write_report($filename, $options, $ss_init_count, $ss_output_count,
#                      $start_cycles, $ss_cycles, 
#                      $work_count, $parsed_output_base_filename);
sub write_report {
    my $filename        = shift || die("No filename passed to write_report");
    my $options         = shift || die("No options passed to write_report");
    my $ss_init_count   = shift; # can produce 0 init outputs (normal case)
    my $ss_output_count = shift || die("No start_cycles passed to write_report");
    my $start_cycles    = shift || die("No start_cycles passed to write_report");
    my $ss_cycles       = shift || die("No steady state cycles passed to write_report");
    my $work_count      = shift || die("No steady state cycles passed to write_report");
    my $flops_count     = shift || die("No flops count passed to write_report");
    my $base_filename   = shift || die("No basefilename passed to write_report");


    # calculate averate cycles per output
    my $average_cycles_per_output = $ss_cycles/$ss_output_count;
    # calculate throughput
    my $tput = ($ss_output_count / $ss_cycles) * 100000;
    $tput = int($tput * 100) / 100;
    # figure out percentage of useful work being performed
    my ($useful_cycles, $available_cycles) = $work_count =~ m/(.*) \/ (.*)/;
    my $utilization_percent = ($useful_cycles / $available_cycles) * 100;
    $utilization_percent = int($utilization_percent * 100) / 100;
    # figure out the mFLOPS number
    my ($foo, $actual_flops, $bar) = $flops_count =~ m/(.*?) (.*?) (.*?)/g;
    my $MFLOPS = ($actual_flops/$ss_cycles) * 250; 
    $MFLOPS = int($MFLOPS * 100) / 100;
    

    my $report = "Autogenerated by reap_results.pl at: " . make_date_stamp() . "\n";
    $report .= "Filename: $filename\n";
    $report .= "Compilation options: $options\n";
    $report .= "Initialization outputs: $ss_init_count\n";
    $report .= "Steady state outputs: $ss_output_count\n";
    $report .= "Cycles until the start of the second steady state iteration: $start_cycles\n";
    $report .= "Average ss cycles: $ss_cycles\n";
    $report .= "Average cycles per output: $average_cycles_per_output\n";
    $report .= "Average throughput (outputs per 10^5 cycles): $tput\n";
    $report .= "Work count: $work_count\n";
    $report .= "Utilization percent per SS cycle: $utilization_percent\n";
    $report .= "FLOPS per steady state cycle: $flops_count\n";
    $report .= "MFLOPS per steady state cycle: $MFLOPS\n";

    $report .= "\n\n\n";

    write_file($report, "$base_filename.report");
}



# Generates a summary file out of all of the reports in a given directory.
# The reports must be in the format generated by write_report
# usage generate_summary($results_directory, $summary_file);
sub generate_summary {
    my $results_directory = shift || die("no results directory passed to generate_summary");
    my $summary_file      = shift || die("no summary filename passed to generate_summary");

    # place where we are going to store the summary.
    # each element is a tab delimited list of numbers
    my @summary_table;
    # add heading
    push(@summary_table,
	 "File\tOptions\tThroughput\tUtilization(percent)\tMFLOPS");


    # get a listing of all the report files in the result directory
    my @files = split("\n", `ls $results_directory/*.report`);
    my $current_file;
    foreach $current_file (@files) {
	my $contents = read_file($current_file);
	# get rid of headings (eg anything upto and including the ": ")
	$contents =~ s/(.*): (.*)/$2/g;
	# parse the contents of the summary
	my ($date, $filename, $options, 
	    $init_outputs, $ss_outputs,
	    $ss_2_start, $avg_ss_cycles,
	    $avg_cyc_per_out, $avg_tput,
	    $work_count, $util_pct,
	    $flops_count, $mflops_count) = split("\n", $contents);

	# add a line in the summary
	push(@summary_table,
	     "$filename\t$options\t$avg_tput\t$util_pct\t$mflops_count");

	
    }
    # write the results out to the summary file
    write_file(join("\n", @summary_table), 
	       $summary_file);
    # write out a gratitous tex file
    write_file(make_tex_table(@summary_table),
	       "$summary_file.tex");
	       
}



###############################################
############## Utility Subroutines#############
###############################################

# makes a date stamp useful for naming results files with
# usage $ds = make_date_stamp()
sub make_date_stamp {
    # run unix "date" command 
    my $date_stamp = `date`;
    # remove new line
    chomp($date_stamp); 
    # remove colons
    $date_stamp =~ s/:/\./g;
    # remove spaces
    $date_stamp =~ s/ /-/g;
    
    return $date_stamp;
}


# reads in a file and returns its contents as a scalar variable
# usage: read_file($filename)
sub read_file {
    my $filename = shift || die ("no filename passed to read_file\n");

    open(INFILE, "<$filename") || die ("could not open $filename");
    
    # save the old standard delimiter
    my $old_delim = local($/);
    # set the standad delimiter to undef so we can read
    # in the file in one go
    local($/) = undef; 

    # read in file contents
    my $file_contents = <INFILE>;
    close(INFILE);
    
    # restore the standard delimiter
    local($/) = $old_delim;

    return $file_contents;
}

# writes the contents of the first scalar argument to the 
# filename in the second argument
# usage: write_file($data, $filename)
sub write_file {
    my $data = shift || die("No data passed to write_file");
    my $filename = shift || die("No filename passed to write_file");
    
    open(OUTFILE, ">$filename");
    print OUTFILE $data;
    close(OUTFILE);
}


# Makes a tex table out of the passed array
# which contains tab separated values. First element 
# contains the titles for the table.
# usage: make_tex_table(@data)
sub make_tex_table {
    my @data = @_;
    

    # set up doc preamble
    my $tex = "\\documentclass{article}\n";
    $tex .= "\\begin{document}\n";

    # set up table header
    my $header = shift(@data); # grab first row (headings)
    my @headings = split("\t",$header);
    $tex .= "\\begin{tabular} {";
    foreach (@headings) {
	$tex .= "l|";
    }
    chop($tex);
    $tex .= "}\n";
    $tex .= join(" & ", @headings) . " \\\\\n";
    $tex .= "\\hline\n";
    
    # now, make the body of the table
    my $current_data_line;
    foreach $current_data_line (@data) {
	my @items = split("\t", $current_data_line);
	$tex .= join(" & ", @items) . " \\\\\n";
    }

    # end the table
    $tex .= "\\end{tabular}\n";


    $tex .= "\\end{document}\n";

    # finally, replace _ with \_ because tex sux
    $tex =~ s/_/\\_/g;
    
    return $tex;
}
    








# wacky perl syntax (included files have to return true...)
1;
