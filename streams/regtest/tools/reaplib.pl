#!/usr/local/bin/perl
# library routines for reaping performance data from
# the streamit compiler.
# $Id: reaplib.pl,v 1.1 2002-07-11 21:01:29 aalamb Exp $

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



# wacky perl syntax
1;
