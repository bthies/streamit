#!/usr/local/bin/perl

# this program executes the specified programs
# Using the following options:
# constprop and unroll 10000
# Alone, with linearreplacement, with frequencyreplacement and with both
#
# Then the script executes the program using a dynamo-rio program(module?)
# which counts the number of flops, fadds and fmuls that occur 
# in the program, recording it to a file for analysis.
#
# This is the latest reincarnation of a continually mutating script
# to gather numbers for linear analysis and replacement.

use strict;

# the streamit frontend, the streamit compiler, the C compiler and dynamorio
my $STREAMIT_FRONTEND = "java streamit.frontend.ToJava --full";
my $STREAMIT_COMPILER = "java -Xmx1500M  at.dms.kjc.Main -s";
my $STREAMIT_GCC      = "gcc -O2 -lm -I/u/aalamb/streams/library/c /u/aalamb/streams/library/c/stream*.c";
my $STREAMIT_DYNAMORIO= "dynamorio";

# the program to use to compare output
my $CMP = "/u/aalamb/streams/regtest/tools/compare_uni/pl";

# the filename to write out the results to.
my $RESULTS_FILENAME = "freq_results.tsv";

# the number of iterations to run the program for
my $NUM_ITERS = 10000;

# array to hold results
my @result_lines;
# heading
push(@result_lines, 
     "Program\ttarget output size\t" .
     "normal flops\tnormal fadds\tnormal fmuls\tnormal outputs\t" .
     "linear flops\tlinear fadds\tlinear fmuls\tlinear outputs\t" .
     "freq flops\tfreq fadds\tfreq fmuls\tfreq outputs\t" .
     "both flops\tboth fadds\tboth fmuls\tboth outputs\t");


# format for script programs is 
#directory:filename
my @programs = (
		".:FIRProgram",
		".:SamplingRateConverter",
		".:FilterBank",
		#".:TargetDetect",
		".:Test",
		);





foreach (@programs) {
    # parse the input into path and program
    my ($path, $base_filename) = split(":");

    # compile normally without frequency replacement
    print "$base_filename(normal):";
    do_compile($path, $base_filename, "--constprop --unroll 100000 --debug");
    
    # figure out how many outputs are produced
    my $normal_outputs = get_output_count($path, $base_filename) * $NUM_ITERS;
    
    # run the dynamo rio test and get back the results
    my $report = run_test($path, $base_filename, "normal", $NUM_ITERS);
    print "\n";
    
    # extract the flops, fadds and fmul count from the report
    my ($normal_flops) =  $report =~ m/saw (.*) flops/;
    my ($normal_fadds) =  $report =~ m/saw (.*) fadds/;
    my ($normal_fmuls) =  $report =~ m/saw (.*) fmuls/;

    # compile with linear replacement
    print "$base_filename(linear):";
    do_compile($path, $base_filename, "--constprop --unroll 100000 --debug --linearreplacement");
    
    # figure out how many outputs are produced
    my $linear_outputs = get_output_count($path, $base_filename) * $NUM_ITERS;
    
    # run the dynamo rio test and get back the results
    $report = run_test($path, $base_filename, "linear", $NUM_ITERS);
    print "\n";
    
    # extract the flops, fadds and fmul count from the report
    my ($linear_flops) =  $report =~ m/saw (.*) flops/;
    my ($linear_fadds) =  $report =~ m/saw (.*) fadds/;
    my ($linear_fmuls) =  $report =~ m/saw (.*) fmuls/;

    
    # for various sizes of target FFT length
    my $MAX_TARGET_SIZE = 4096;
    my $targetFFTSize;
    for ($targetFFTSize=1; $targetFFTSize<($MAX_TARGET_SIZE+1); $targetFFTSize*=2) {
	
        # now, do the compilation with the frequency replacement
	print "$base_filename(freq, $targetFFTSize):";
	do_compile($path, $base_filename, "--constprop --unroll 100000 --debug --frequencyreplacement --targetFFTSize $targetFFTSize");
	
	# figure out how many outputs are produced 
	my $freq_outputs = get_output_count($path, $base_filename) * $NUM_ITERS;
	
	# run the dynamo rio test and get back the results
	$report = run_test($path, $base_filename, "freq$targetFFTSize", $NUM_ITERS);
	print "\n";
	
	# extract the flops, fadds and fmul count from the report
	my ($freq_flops) =  $report =~ m/saw (.*) flops/;
	my ($freq_fadds) =  $report =~ m/saw (.*) fadds/;
	my ($freq_fmuls) =  $report =~ m/saw (.*) fmuls/;

        # now, do the compilation with linear replacement followed by frequency replacement
	print "$base_filename(both, $targetFFTSize):";
	do_compile($path, $base_filename, "--constprop --unroll 100000 --debug --linearreplacement --frequencyreplacement --targetFFTSize $targetFFTSize");
	
	# figure out how many outputs are produced 
	my $both_outputs = get_output_count($path, $base_filename) * $NUM_ITERS;
	
	# run the dynamo rio test and get back the results
	$report = run_test($path, $base_filename, "both$targetFFTSize", $NUM_ITERS);
	print "\n";
	
	# extract the flops, fadds and fmul count from the report
	my ($both_flops) =  $report =~ m/saw (.*) flops/;
	my ($both_fadds) =  $report =~ m/saw (.*) fadds/;
	my ($both_fmuls) =  $report =~ m/saw (.*) fmuls/;

	
	push(@result_lines, 
	     "$base_filename\t$targetFFTSize\t".
	     "$normal_flops\t$normal_fadds\t$normal_fmuls\t$normal_outputs\t" .
	     "$linear_flops\t$linear_fadds\t$linear_fmuls\t$linear_outputs\t" .
	     "$freq_flops\t$freq_fadds\t$freq_fmuls\t$freq_outputs\t" .
	     "$both_flops\t$both_fadds\t$both_fmuls\t$both_outputs\t");

    }

    # now, we should compare all of the output files against the normal output
    # and die horribly if there are any errors.
    my @output_files = split("\n", `ls *.output`);
    my $current_output_file;
    foreach $current_output_file (@output_files) {
	chomp($current_output_file);
	my $result = `$CMP $base_filename-normal.output $current_output_file`;
	if ($result) {
	    die ("Error comparing $base_filename-normal.outout and $current_output_file: $result");
	} else {
	    print "(verified $current_output_file)";
	}
    }
    print "\n";
    # now, remove all of the output files
    `rm -f *.output`;
}


# now, when we are done with all of the tests, write out the results to a tsv file.
print "writing tsv";
open (RFILE, ">$RESULTS_FILENAME");
print RFILE join("\n", @result_lines);
close RFILE;
print "done\n";


########
# Subroutine to compile the specified file.
# usage: do_compile($path, $filename, $compiler_options);
########
sub do_compile {
    my $new_path      = shift || die ("no path passed to do_compile.");
    my $filename_base = shift || die ("no filename passed to do_compile.");
    my $options       = shift;

    # run streamit compiler to generate C code.
    print "(java->c)";
    `$STREAMIT_COMPILER $options $new_path/$filename_base.java >& $new_path/$filename_base.c`;
    # compile the C code to generate an executable
    print "(c->exe)";
    `$STREAMIT_GCC $new_path/$filename_base.c -o $new_path/$filename_base.exe`;

}

#######
# Subroutine to execute the program with dynamo
sub run_test {
    my $new_path      = shift || die ("no new path passed");
    my $filename_base = shift || die ("no filename base passed");
    my $postfix       = shift || die ("no postfix specified.");
    my $iters         = shift || die ("no iters passed");

    # remove the old countflops file (if things in rio don't go well, we want to report real results)
    print `rm -f $new_path/countflops.log`;

    # run dynamo rio (with the assumed countflops module installed)
    print "(dynamo $iters)";
    print `cd $new_path; $STREAMIT_DYNAMORIO $filename_base.exe -i $iters >& $filename_base-$postfix.output`;

    # get the report from the countflops.log file and clean up
    my $report = read_file("$new_path/countflops.log");
    return($report);

}


# Gets the number of outputs produced by one iteration of the stream steady state
sub get_output_count {
    my $path = shift || die ("no path");
    my $filename_base = shift || die ("no filename base");

    print "(output count)";
    
    # run the program for one iter piping its output to wc
    # whose output we will parse and return a value.
    my $wc_results = `$path/$filename_base.exe -i 1 | wc -l`;
    my ($count) = $wc_results =~ m/(\d*)\n/gi;
    return $count;
}

# get the actual N of the output.
sub get_N {
    my $path = shift || die ("no path");
    my $filename_base = shift || die ("no filename base");
    # read in the c file and extract the information from the comments.
    my $contents = read_file("$path/$filename_base.c");
    my ($N) = $contents =~ m/N=(\d*)/gi;
    return $N;
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
