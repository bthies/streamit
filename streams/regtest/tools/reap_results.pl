#!/usr/local/bin/perl
#
# Script to run and reap raw results for asplos paper. Eventually might
# become more general purpose (eg integrated into regtest).
#
# Usage: reap_results.pl
# $Id: reap_results.pl,v 1.2 2002-07-11 21:01:28 aalamb Exp $

# The basic idea is for each directory and file, 
# run the streamit compiler targeting raw, modify the 
# Makefile.streamit to run the simulator to produce 
# a blood-graph and save the bloodgraph to an appropriate location.
# (other performance information might also be gleaned from the 
# output of the raw simulator


use strict;

require "reaplib.pl";

my $result_directory = "/u/aalamb/results";
my $examples_dir = "/u/aalamb/streams/docs/examples/hand";
my $apps_dir     = "/u/aalamb/streams/apps";

# format: "directory:filename:initialization count:steady state count"
my @results_wanted = ("$examples_dir/fft:FFT_inlined.java:--raw 4 --partition:0:32",
		      "$examples_dir/fft:FFT_inlined.java:--raw 4 --partition --fusion:0:32",
		      "$examples_dir/fft:FFT_inlined.java:--raw 4 --partition --constprop:0:32",
		      "$examples_dir/fft:FFT_inlined.java:--raw 4 --partition --constprop --fusion:0:32",
		      "$examples_dir/fft:FFT_inlined.java:--raw 8:0:32",
		      "$examples_dir/fft:FFT_inlined.java:--raw 8 --fusion:0:32",
		      "$examples_dir/fft:FFT_inlined.java:--raw 8 --constprop:0:32",
		      "$examples_dir/fft:FFT_inlined.java:--raw 8 --constprop --fusion:0:32",
		      "$examples_dir/fib:Fib.java:--raw 4:0:1",
		      "$examples_dir/fib:Fib.java:--raw 4 --partition:0:1",
		      "$examples_dir/fib:Fib.java:--raw 4 --partition --fusion:0:1",
		      "$examples_dir/fib:Fib.java:--raw 4 --partition --fusion --constprop:0:1",
		      
		      #"$apps_dir/FMRadio:LinkedFMTest.java:--raw 8 --partition"
		      );


# process each entry in our results wanted list
my $temp_num = 1;
foreach(@results_wanted) {
    my ($dir, $filename, $options, $init_output_count, $ss_output_count) = split(/:/);

    # fork the process to run the simulator as its own process (and take advantage
    # of the crazy cag machines
    if (fork() == 0) {
	# we are the child
	do_child_work($temp_num, $dir, $filename, $options, $init_output_count, $ss_output_count);
    } # otherwise we are the parent, and continue spawining kids
    
    print "parent: spawned child number ($temp_num)\n";
	
    # Increment count
    $temp_num++;
}

# wait for all children to be done
print "parent: waiting for children.\n";
my $child_pid = wait();
while ($child_pid != -1) {
    print "parent: child ($child_pid) finished.\n";
    $child_pid = wait();
}
print "parent: done waiting.\n";

#remove all old temp directories
#print "removing directories\n"; 
#print `rm -rf /tmp/resultTemp*`;



########################### Subroutines ##########################

# 
# Does the actual work of compiling, making graphs, etc.
# usage: do_child_work($temp_num, $source_dir, $filename, $options, $ss_init_count, $ss_output_count)
sub do_child_work {
    my $temp_num = shift || die("No temp num to do_child_work");
    my $dir      = shift || die("No source dir to do_child_work");
    my $filename = shift || die("No filename to do_child_work");
    my $options  = shift || die("No options to do_child_work");
    my $ss_init_count  = shift; # || die("No steady state init count to do_child_work");
    my $ss_output_count  = shift || die("No steady state output count to do_child_work");

    # make the temp directory
    my $temp_dir = "/tmp/resultTemp$temp_num";
    print "child ($temp_num): creating $temp_dir" . `mkdir $temp_dir/` . "\n";

    # copy source file to temp dir
    print "child ($temp_num): copying source $filename to $temp_dir" . `cp $dir/$filename $temp_dir/` . "\n";

    # do the compilation (eg streamit-->raw)
    print "child ($temp_num): compiling $filename to $dir with $options\n";
    compile_to_raw($temp_dir, $filename, $options);
    print "child ($temp_num): done compiling.\n";

    # create the appropriate filenames for the results    
    my $date_stamp = make_date_stamp();
    $options =~ s/ //g; #remove spaces
    my $blood_graph_filename = "$result_directory/$filename$options-$date_stamp.png";
    my $raw_output_filename = "$result_directory/$filename$options-$date_stamp.output";
    my $parsed_output_base_filename = "$result_directory/$filename$options-$date_stamp";
    my $gnu_plot_filename = "$result_directory/$filename$options-$date_stamp.output.eps";


    # make the blood graph by running btl (and save the output)
    print "child ($temp_num): running btl on $filename\n";
    my $btl_results = run_btl($temp_dir);
    write_file($btl_results, $raw_output_filename);
    print "child ($temp_num): done with btl\n";

    print "child ($temp_num): parsing initial btl output.\n";
    my $parsed_cycles = parse_results($parsed_output_base_filename, 
				      $btl_results, 
				      $ss_init_count, 
				      $ss_output_count);
    print "child ($temp_num): done parsing output.\n";

    # parse the starting cycle for second steady state iteration, and how many cycles per iteration
    my ($start_cycles, $ss_cycles) = split(":", $parsed_cycles);
    print "child ($temp_num): making blood graph for $filename (start=$start_cycles, 1 ss=$ss_cycles)\n";
    make_blood_graph($temp_dir, "$blood_graph_filename", $start_cycles, $ss_cycles);
    print "child ($temp_num): done making blood graph for $filename\n";

    # save the output .dot files from the directory
    print "child ($temp_num): saving dot files and writing summary for $filename\n";
    save_dot_files($temp_dir, $parsed_output_base_filename);
    write_summary($filename, $options, 
		  $ss_init_count, $ss_output_count,
		  $start_cycles, $ss_cycles, 
		  $parsed_output_base_filename);
    print "child ($temp_num): done saving dot files and writing summary\n";    

    #print "child ($temp_num): generating plot $gnu_plot_filename.\n";
    #generate_plot($parsed_output_filename, $gnu_plot_filename);
    #print "child ($temp_num): done creating $gnu_plot_filename.\n";

    print "child ($temp_num): fixing blood graph for $filename (rotating, flipping)\n";    
    fix_blood_graph($blood_graph_filename);
    print "child ($temp_num): done fixing bloodgraph\n";
    exit;
}





# runs the compiler on the target file with the specified compiler
# options turned on
# usage compile_to_raw($working_dir, $filename)
sub compile_to_raw {
    my $directory = shift || die ("no directory passed to compile_to_raw\n");
    my $filename  = shift || die ("no filename  passed to compile_to_raw\n");
    my $options   = shift || die ("no options  passed to compile_to_raw\n");

    my $compiler_command = "java -Xmx1024M at.dms.kjc.Main -s $options";

    # execute the streamit compiler
    #print "compiling $filename with $options to $directory\n";
    my $compiler_results = `cd $directory; $compiler_command $directory/$filename`;
    #print "Compiler results: \n\n$compiler_results\n";
}


# runs btl in the specified directory by running make -f Makefile.streamit
# usage: run_btl($directory)
sub run_btl {
    my $directory = shift || die("no directory passed to run_btl");
    return `cd $directory; make -f Makefile.streamit run`;
}


# creates a modified makefile which will generate a bloodgraph
# usage: make_blood_graph($working_dir, $blood_graph_name, $start_cycles, $ss_cycles)
# where blood_graph_name is the (absolute) name of the file where the blood graph should
# be stored, $start_cycles is how many cycles to run before making the blood graph
# and $ss_cycles is the number of cycles needed to produce one steady state iteration
sub make_blood_graph {
    my $directory = shift || die ("no directory passed to make_blood_graph\n");
    my $graph_filename  = shift || die ("no filename  passed to make_blood_graph\n");
    my $start_cycles  = shift || die ("no start cycles  passed to make_blood_graph\n");
    my $ss_cycles  = shift || die ("no ss cycles  passed to make_blood_graph\n");
    
    # make 1.1 steady state cycles worth of blood graph
    $ss_cycles = $ss_cycles * 1.1;

    # first of all, create a new makefile that contains the command to make the bloodgraph
    my $blood_makefile_name = make_blood_makefile($directory,
						  "Makefile.streamit", 
						  $graph_filename,
						  $start_cycles,
						  $ss_cycles);

    # run the makefile to generate the blood graph
    my $sim_output = `cd $directory; make -f $blood_makefile_name run;`;
    return $sim_output;
    #print $sim_output;
    
}


# copies the dot files that are generated by the raw 
# compiler path into the results directory so that they can be referenced when
# comparing numbers
# usage: save_dot_files($temp_dir, $base_file_name);
sub save_dot_files {
    my $temp_dir = shift || die ("No temp directory passed to save_dot_files");
    my $base_filename = shift || die ("No base filename passed to save_dot_files");

    # just copy the files
    print `cp $temp_dir/before.dot $base_filename.before.dot`;
    print `cp $temp_dir/after.dot $base_filename.after.dot`;
    print `cp $temp_dir/layout.dot $base_filename.layout.dot`;
}

# writes an executive summary of the data (eg an entry in the asplos table)
# usage: write_summary($filename, $options, $ss_init_count, $ss_output_count,
#                      $start_cycles, $ss_cycles, $parsed_output_base_filename);
sub write_summary {
    my $filename        = shift || die("No filename passed to write_summary");
    my $options         = shift || die("No options passed to write_summary");
    my $ss_init_count   = shift; # can produce 0 init outputs
    my $ss_output_count = shift || die("No start_cycles passed to write_summary");
    my $start_cycles    = shift || die("No start_cycles passed to write_summary");
    my $ss_cycles       = shift || die("No steady state cycles passed to write_summary");
    my $base_filename   = shift || die("No basefilename passed to write_summary");

    my $report = "";
    $report .= "Autogenerated by reap_results.pl at " . make_date_stamp() . "\n";
    $report .= "Filename: $filename\nOptions: $options\n";
    $report .= "Number of outputs generated by initialization: $ss_init_count\n";
    $report .= "Number of outputs generated by steady state: $ss_output_count\n";
    $report .= "Cycles until the start of the second steady state iteration: $start_cycles\n";
    $report .= "Average Steady State Cycles: $ss_cycles\n";
    $report .= "Average Cycles per output: " . ($ss_cycles/$ss_output_count) . "\n";
    my $tput = ($ss_output_count / $ss_cycles) * 100000;
    $report .= "Average Throughput (outputs per 10^5 cycles): $tput\n";

    $report .= "\n\n\n";

    write_file($report, "$base_filename.report");
}

# Generates a figure
# Usage: generate_plot($input_filename, $output_filename)
sub generate_plot {
    my $input_filename  = shift || die ("no input filename to generate_plot");
    my $output_filename = shift || die ("no output filename to generate_plot");
    
    #first, create the gnuplot script
    my $script_filename = "input_filename.gnuplot_script";
    my $script;
    #$script .= "set title "FFT_inlined";
    $script .= "set xlabel \"cycles (eg time)\"\n";
    $script .= "set ylabel \"output produced\"\n";
    $script .= "set terminal postscript eps color\n";
    $script .= "set output \"$output_filename\"\n";
    $script .= "plot \"$input_filename\"\n";
    write_file($script, $script_filename);

    # execute gnuplot
    `gnuplot $script_filename`;

    # delete the script
    `rm -rf $script_filename`;
}


# Prepares the bloodgraph for publication/viewing
# (eg hflip and rotate by 270)
# usage: fix_blood_graph(blood_graph_name)
sub fix_blood_graph {
    my $blood_graph_name = shift || die("No blood graph filename passed.");
    
    # use convert to flip horizontally and rotate
    print `convert -flop -rotate 270 $blood_graph_name $blood_graph_name`;
}
    



# creates a blood graph makfile from the makefile created by streamit
# usage: $new_makefile_name = make_blood_makefile($dir, $old_makefile_name, $graphname, $start_cycles, $ss_cycles)
# returns the new makefile name (in the $dir directory)
sub make_blood_makefile {
    my $directory = shift || die("No directory passed to make_blood_makefile");
    my $old_makefile = shift || die("No old makefile name passed to make_blood_makefile");
    my $graph_filename = shift || die("No graph file name passed to make_blood_makefile");
    my $start_cycles = shift || die("No start cycles passed to make_blood_makefile");
    my $ss_cycles = shift || die("No ss cycles passed to make_blood_makefile");

    # Make sure that both start and ss cycles are integers
    $start_cycles = int($start_cycles);
    $ss_cycles    = int($ss_cycles);

    # read in the contents of the old makefile
    my $makefile_contents = read_file("$directory/$old_makefile");
    
    # replace the cycle-count line with a sim command line as well
    # which causes the simulation to run as normal and then to print the blood graph
    # of the remaining 5000 cycles
    $makefile_contents =~ s/(SIM-CYCLES = .*)/$1\nSIM-COMMAND = step($start_cycles); graphical_trace_ppm(\\\"$graph_filename\\\", $ss_cycles);\n/g;

    # determine the new makefile name
    my $new_makefile = "$old_makefile.blood"; 
    # write out the modified makefile to the new file
    write_file($makefile_contents, "$directory/$new_makefile");

    # return the new filename
    return $new_makefile;
}

