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
#
# If a filename is passed as an argument, this script will use that as
# the input file specifying which programs to run. If no argument is passed,
# this script just uses the default one.

use strict;

# the streamit frontend, the streamit compiler, the C compiler and dynamorio
my $STREAMIT_FRONTEND    = "java streamit.frontend.ToJava --full";
my $STREAMIT_COMPILER    = "java -Xmx1500M  at.dms.kjc.Main -s";
my $STREAMIT_GCC         = "gcc -O2 -lm -I/u/aalamb/streams/library/c -L/u/aalamb/streams/library/c";
my $STREAMIT_GCC_POSTFIX = "-lstreamit -lsrfftw -lsfftw -lm";
my $STREAMIT_DYNAMORIO   = "dynamorio";

# the program to use to compare output
my $CMP = "/u/aalamb/streams/regtest/tools/compare_uni/pl";

# the filename to write out the results to.
my $RESULTS_FILENAME = "freq_results.tsv";

# the number of iterations to run the program for
my $NUM_ITERS = 10000;

# format for script programs is 
#directory:filename:max size for frequency
my @programs;
# if we have a filename, read it in and use that, otherwise use
if (@ARGV) {
    @programs = split("\n", read_file(shift(@ARGV)));
} else {
    @programs = (
		 ".:FIRProgram",
		 #".:SamplingRateConverter",
		 #".:FilterBank",
		 #".:TargetDetect",
		 #".:LinkedFMTest",
		 #".:CoarseSerializedBeamFormer",
		 #".:OneBitDToA",
		 );
}




# array to hold results
my @result_lines;
# heading
push(@result_lines, 
     "Program\t" .
     "normal flops\tnormal fadds\tnormal fmuls\tnormal outputs\t" .
     "linear flops\tlinear fadds\tlinear fmuls\tlinear outputs\t" .
     "freq 3 flops\tfreq 3 fadds\tfreq 3 fmuls\tfreq 3 outputs\t" .
     "both flops\tboth fadds\tboth fmuls\tboth outputs\t" .
     "redund flops\tredund fadds\tredund fmuls\tredund outputs\t");

# determine the next available results directory (eg results0, results1, etc.)
my $results_dir_num = 0;
while (-e "/tmp/freqResults$results_dir_num") {$results_dir_num++;}
my $results_dir = "/tmp/freqResults$results_dir_num";
print `mkdir $results_dir`;

my $current_program;
foreach $current_program (@programs) {
    #ignore blank lines
   if (not $current_program) {next;}
    # parse the input into path, program and max frequency size
    my ($path, $base_filename) = split(":", $current_program);

    # copy the input program into the new results dir
    print `cp $path/$base_filename.java $results_dir`;

    # update the path
    $path = $results_dir;
    
    # compile normally without frequency replacement
   my ($normal_outputs, $normal_flops, 
       $normal_fadds, $normal_fmuls) = do_test($path, $base_filename,
					       "--unroll 100000 --debug", 
					       "$base_filename(normal)");
   save_output($path, $base_filename, "normal");

   # compile with linear replacement
   my ($linear_outputs, $linear_flops, 
       $linear_fadds, $linear_fmuls) = do_test($path, $base_filename, 
					       "--unroll 100000 --debug --linearreplacement", 
					       "$base_filename(linear)");
   save_output($path, $base_filename, "linear");
   

   # now, do the compilation with (leet fftw) frequency replacement
   my ($freq3_outputs, $freq3_flops, 
       $freq3_fadds, $freq3_fmuls) = do_test($path, $base_filename, 
					     "--unroll 100000 --debug --frequencyreplacement 3",
					     "$base_filename(freq 3)");
   save_output($path, $base_filename, "freq3");
   
   # now, run with both optimizations (fftw and linear)
   my ($both_outputs, $both_flops, 
       $both_fadds, $both_fmuls) = do_test($path, $base_filename, 
					   "--unroll 100000 --debug --linearreplacement --frequencyreplacement 3",
					   "$base_filename(both)");
   save_output($path, $base_filename, "both");

   # now, run with redundant elimination
   my ($redund_outputs, $redund_flops, 
       $redund_fadds, $redund_fmuls) = do_test($path, $base_filename, 
					       "--unroll 100000 --debug --redundantreplacement",
					       "$base_filename(redund)");
   save_output($path, $base_filename, "redund");

   
   my $new_data_line = 	     ("$base_filename\t".
			      "$normal_flops\t$normal_fadds\t$normal_fmuls\t$normal_outputs\t" .
			      "$linear_flops\t$linear_fadds\t$linear_fmuls\t$linear_outputs\t" .
			      "$freq3_flops\t$freq3_fadds\t$freq3_fmuls\t$freq3_outputs\t" .
			      "$both_flops\t$both_fadds\t$both_fmuls\t$both_outputs\t" .
			      "$redund_flops\t$redund_fadds\t$redund_fmuls\t$redund_outputs\t");
   
   open (MHMAIL, "|mhmail aalamb\@mit.edu -s \"results mail: ($path,$base_filename)\"");
   print MHMAIL $new_data_line;
   close(MHMAIL);
   
   push(@result_lines, $new_data_line);

}


# now, when we are done with all of the tests, write out the results to a tsv file.
print "(writing tsv)";
open (RFILE, ">$results_dir/$RESULTS_FILENAME");
print RFILE join("\n", @result_lines);
close RFILE;
print "(done)";


print "(sending mail)";
open (MHMAIL, "|mhmail aalamb\@mit.edu -s \"Overall results mail\"");
print MHMAIL join("\n", @result_lines);
close(MHMAIL);
print "(done)\n";


#########
# subroutine to do a compile test, and parse results.
# Return value is (outputs, flops, fadds, fmuls).
# do_test($path, $base_filename, $options, $descr)
#########
sub do_test {
    my $path = shift || die ("no flops");
    my $base_filename = shift || die ("no base");
    my $options = shift || die("no options");
    my $descr = shift || die ("no description");
    
    # compile with specified options
    print "$descr:";
    do_compile($path, $base_filename, $options);
    
    # figure out how many outputs are produced
    my $outputs = get_output_count($path, $base_filename, $NUM_ITERS);
    
    # run the dynamo rio test and get back the results
    my $report = run_rio($path, $base_filename, $descr, $NUM_ITERS);
    print "\n";
    
    # extract the flops, fadds and fmul count from the report
    my ($flops) =  $report =~ m/saw (.*) flops/;
    my ($fadds) =  $report =~ m/saw (.*) fadds/;
    my ($fmuls) =  $report =~ m/saw (.*) fmuls/;

    return ($outputs, $flops, $fadds, $fmuls);
}

#############
# This subroutine saves the current c and exe file in path
# by appending the tag to their names.
#############
sub save_output {
    my $path          = shift || die("no path");
    my $base_filename = shift || die("no base filename");
    my $tag           = shift || die("no tag");
    
    #print "(saving $tag)\n";
    # copy c file
    #print `cp $path/$base_filename.c $path/$base_filename-$tag.c`;
    # copy exe file
    #print `cp $path/$base_filename.exe $path/$base_filename-$tag.exe`;
}





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
    `cd $new_path; $STREAMIT_COMPILER $options $filename_base.java >& $filename_base.c`;
    # compile the C code to generate an executable
    print "(c->exe)";
    `cd $new_path; $STREAMIT_GCC $filename_base.c -o $filename_base.exe $STREAMIT_GCC_POSTFIX`;

}

#######
# Subroutine to execute the program with dynamo
sub run_rio {
    my $new_path      = shift || die ("no new path passed");
    my $filename_base = shift || die ("no filename base passed");
    my $postfix       = shift || die ("no postfix specified.");
    my $iters         = shift || die ("no iters passed");

    # remove the old countflops file (if things in rio don't go well, we want to report real results)
    print `rm -f $new_path/countflops.log`;

    # run dynamo rio (with the assumed countflops module installed)
    print "(dynamo $iters)";
    print `cd $new_path; $STREAMIT_DYNAMORIO $filename_base.exe -i $iters >& /dev/null`;

    # get the report from the countflops.log file and clean up
    my $report = read_file("$new_path/countflops.log");
    return($report);

}


# Gets the number of outputs produced by executing
# the stream for the specified number of iterations
# usage: get_output_count($path, $filenamebase, $num_iters)
sub get_output_count {
    my $path = shift || die ("no path");
    my $filename_base = shift || die ("no filename base");
    my $num_iters = shift || die ("no iter count ");

    print "(output count)";
    
    # run the program for one iter piping its output to wc
    # whose output we will parse and return a value.
    my $wc_results = `$path/$filename_base.exe -i $num_iters | wc -l`;
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
