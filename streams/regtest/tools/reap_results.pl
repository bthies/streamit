#!/usr/local/bin/perl
#
# Script to run and reap raw results for asplos paper. Eventually might
# become more general purpose (eg integrated into regtest).
#
# Usage: reap_results.pl

# The basic idea is for each directory and file, 
# run the streamit compiler targeting raw, modify the 
# Makefile.streamit to run the simulator to produce 
# a blood-graph and save the bloodgraph to an appropriate location.
# (other performance information might also be gleaned from the 
# output of the raw simulator


use strict;

#default compiler options to use
my $DEFAULT_OPTIONS = "--raw 4";

my $result_directory = "/u/aalamb/results";
my $examples_dir = "/u/aalamb/streams/docs/examples/hand";
my $apps_dir     = "/u/aalamb/streams/apps";
my $asplos_dir   = "/u/aalamb/streams/apps/asplos-apps";

my @results_wanted = (#"$examples_dir/fft:FFT_inlined.java:--raw 4",
		      #"$examples_dir/fft:FFT_inlined.java:--raw 4 --partition",
		      "$examples_dir/fib:Fib.java:--raw 4",
		      "$examples_dir/fib:Fib.java:--raw 4 --fusion",
		      "$examples_dir/fib:Fib.java:--raw 4 --partition",
		      "$examples_dir/fib:Fib.java:--raw 8",

		      #"$apps_dir/BeamFormer-fine:BeamFormer.java",
		      #"$apps_dir/FMRadio:LinkedFMTest.java:--raw 8",
		      #"$apps_dir/FMRadio:LinkedFMTest.java:--raw 8 --partition"
		      );


# process each entry in our results wanted list
my $temp_num = 0;
foreach(@results_wanted) {
    my ($dir, $filename, $options) = split(/:/);
    # make the temp directory
    my $temp_dir = "/tmp/resultTemp$temp_num";
    print `mkdir $temp_dir`;

    # copy source file to temp dir
    print `cp $dir/$filename $temp_dir/`;

    # do the compilation
    compile_to_raw($temp_dir, $filename, $options);
    my $date_stamp = make_date_stamp();

    # reg rid of spaces in the options
    $options =~ s/ //g;

    # fork the process to run the simulator as its own process (and take advantage
    # of the crazy cag machines
    if (fork() == 0) {
	# we are the child
	my $blood_graph_filename = "$result_directory/$filename$options-$date_stamp.png";
	print "child ($temp_num): $temp_dir: creating: $blood_graph_filename\n";
	make_blood_graph($temp_dir, "$blood_graph_filename");
	print "child ($temp_num): done making $blood_graph_filename\n";
	fix_blood_graph($blood_graph_filename);
	print "child ($temp_num): done fixing $blood_graph_filename\n";
	exit;
    } # otherwise we are the parent, and continue spawining kids
	
    # Increment count
    $temp_num++;
}

# wait for all children to be done
print "parent: waiting for children.\n";
wait();
print "parent: done waiting.\n";

#remove all old temp directories
print "removing directories"; 
print `rm -rf /tmp/resultTemp*`;




########################### Subroutines ##########################

# runs the compiler on the target file with the specified compiler
# options turned on
# usage compile_to_raw($working_dir, $filename)
sub compile_to_raw {
    my $directory = shift || die ("no directory passed to compile_to_raw\n");
    my $filename  = shift || die ("no filename  passed to compile_to_raw\n");
    my $options   = shift || $DEFAULT_OPTIONS;

    my $compiler_command = "java -Xmx1024M at.dms.kjc.Main -s $options";

    # execute the streamit compiler
    print "compiling $filename with $options to $directory\n";
    my $compiler_results = `cd $directory; $compiler_command $directory/$filename`;
    #print "Compiler results: \n\n$compiler_results\n";
}

# creates a modified makefile which will generate a bloodgraph
# usage: make_blood_graph($working_dir, $blood_graph_name)
# where blood_graph_name is the (absolute) name of the file where the blood graph should
# be stored
sub make_blood_graph {
    my $directory = shift || die ("no directory passed to make_blood_graph\n");
    my $graph_filename  = shift || die ("no filename  passed to make_blood_graph\n");
    
    # first of all, create a new makefile that contains the command to make the bloodgraph
    my $blood_makefile_name = make_blood_makefile($directory,
						  "Makefile.streamit", 
						  $graph_filename);

    # run the makefile to generate the blood graph
    my $sim_output = `cd $directory; make -f $blood_makefile_name run;`;
    #print $sim_output;
    
    # now, run a regexp pattern match on the raw output
    # to extract the cycle counts and the actual output from the simulator
    # (how is that for an awful regular expression?)
    my @items = $sim_output =~ m/\[(.+\:)\s(.+)]\:\s(.*)\n/g;
    while(@items) {
	my $raw_number = shift(@items); # this is the first "10:" number
	my $cycle = shift(@items);
	my $output = shift(@items);
	#print "cycle: $cycle output: $output\n";
    }
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
# usage: $new_makefile_name = make_blood_makefile($dir, $old_makefile_name, $graphname)
# returns the new makefile name (in the $dir directory)
sub make_blood_makefile {
    my $directory = shift || die("No directory passed to make_blood_makefile");
    my $old_makefile = shift || die("No old makefile name passed to make_blood_makefile");
    my $graph_filename = shift || die("No graph file name passed to make_blood_makefile");

    # read in the contents of the old makefile
    my $makefile_contents = read_file("$directory/$old_makefile");
    
    # replace the cycle-count line with a sim command line as well
    # which causes the simulation to run as normal and then to print the blood graph
    # of the remaining 5000 cycles
    $makefile_contents =~ s/(SIM-CYCLES = .*)/$1\nSIM-COMMAND = step(\$(STARTUP-CYCLES)); step(\$(SIM-CYCLES)); graphical_trace_ppm(\\\"$graph_filename\\\", 5000);\n/g;

    # determine the new makefile name
    my $new_makefile = "$old_makefile.blood"; 
    # write out the modified makefile to the new file
    write_file($makefile_contents, "$directory/$new_makefile");

    # return the new filename
    return $new_makefile;
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

