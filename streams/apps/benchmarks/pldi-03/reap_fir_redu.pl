#!/usr/local/bin/perl

# this script execites the the Test program with various lengths of FIR filter
# and the linear redundancy replacer to see what the effect of scaling is on the
# operation reductions.

use strict;

# the streamit frontend, the streamit compiler, the C compiler and dynamorio
my $STREAMIT_COMPILER    = "java -Xmx1500M  at.dms.kjc.Main -s";
my $STREAMIT_GCC         = "gcc -O2 -I/u/aalamb/streams/library/c -L/u/aalamb/streams/library/c";
my $STREAMIT_GCC_POSTFIX = "-lstreamit -lsrfftw -lsfftw -lm";
my $STREAMIT_DYNAMORIO   = "dynamorio";

my $STANDARD_OPTIONS = "--unroll 100000 --debug";
my $REDU_OPTIONS     = "--redundantreplacement";

# the filename to write out the results to. (append first command line arg to name)
my $RESULTS_FILENAME = "freq_fir_results_redu.tsv";

# the number of iterations to run the program for
my $NUM_ITERS = 10000;

my $PROGRAM_NAME = "Test";

# array to hold results
my @result_lines;
# heading
push(@result_lines, 
     "Program\tFIR size\t" .
     "normal flops\tnormal fadds\tnormal fmuls\tnormal outputs\t" .
     "redu flops\tredu fadds\tredu fmuls\tredu outputs\t");

# generate the java program from the streamit syntax
print `rm -f $PROGRAM_NAME.java`;
print `make $PROGRAM_NAME.java`;


my $i;
# for various FIR lengths
my @fir_lengths;
for($i=1; $i<64; $i*=sqrt(2)) {
    push(@fir_lengths, int($i));
}
@fir_lengths = ();
for ($i=128; $i<256; $i++) {
    push(@fir_lengths, int($i));
}

my $firLength;
foreach $firLength (@fir_lengths) {
    # modify the program for the appropriate FIR length
    set_fir_length("$PROGRAM_NAME.java", $firLength);

    # compile normally
    print "$PROGRAM_NAME($firLength, normal):";
    do_compile(".", $PROGRAM_NAME, "$STANDARD_OPTIONS");
    
    # figure out how many outputs are produced 
    my $normal_outputs = get_output_count(".", $PROGRAM_NAME) * $NUM_ITERS;
    
    # run the dynamo rio test and get back the results
    my $report = run_test(".", $PROGRAM_NAME, $NUM_ITERS);
    print "\n";
    
    # extract the flops, fadds and fmul count from the report
    my ($normal_flops) =  $report =~ m/saw (.*) flops/;
    my ($normal_fadds) =  $report =~ m/saw (.*) fadds/;
    my ($normal_fmuls) =  $report =~ m/saw (.*) fmuls/;
    

    print "$PROGRAM_NAME($firLength, redundant):";
    do_compile(".", $PROGRAM_NAME, "$STANDARD_OPTIONS $REDU_OPTIONS");
    
    # figure out how many outputs are produced 
    my $redu_outputs = get_output_count(".", $PROGRAM_NAME) * $NUM_ITERS;
	
    # run the dynamo rio test and get back the results
    $report = run_test(".", $PROGRAM_NAME, $NUM_ITERS);
    print "\n";
    
    # extract the flops, fadds and fmul count from the report
    my ($redu_flops) =  $report =~ m/saw (.*) flops/;
    my ($redu_fadds) =  $report =~ m/saw (.*) fadds/;
    my ($redu_fmuls) =  $report =~ m/saw (.*) fmuls/;

    push(@result_lines, 
	 "$PROGRAM_NAME\t$firLength\t".
	 "$normal_flops\t$normal_fadds\t$normal_fmuls\t$normal_outputs\t" .
	 "$redu_flops\t$redu_fadds\t$redu_fmuls\t$redu_outputs\t");
}


# now, when we are done with all of the tests, write out the results to a tsv file.
print "writing tsv";
open (RFILE, ">$RESULTS_FILENAME");
print RFILE join("\n", @result_lines);
close RFILE;
print "done\n";


########
# Subroutine to compile the specified file.
# usage: do_test($path, $filename, $compiler_options);
########
sub do_compile {
    my $new_path     = shift || die ("no path passed to do_test.");
    my $filename_base = shift || die ("no filename passed to do_test.");
    my $options  = shift;

    # run streamit compiler to generate C code.
    print "(java->c)";
    `$STREAMIT_COMPILER $options $new_path/$filename_base.java >& $new_path/$filename_base.c`;
    # compile the C code to generate an executable
    print "(c->exe)";
    `$STREAMIT_GCC $new_path/$filename_base.c $STREAMIT_GCC_POSTFIX -o $new_path/$filename_base.exe`;

}

#######
# Subroutine to execute the program with dynamo
sub run_test {
    my $new_path = shift || die ("no new path passed");
    my $filename_base = shift || die("no filename base passed");
    my $iters = shift || die ("no iters passed");

    # remove the old countflops file (if things in rio don't go well, we want to report real results)
    `rm -f $new_path/countflops.log`;

    # run dynamo rio (with the assumed countflops module installed)
    print "(dynamo $iters)";
    `cd $new_path; $STREAMIT_DYNAMORIO $new_path/$filename_base.exe -i $iters >& /dev/null`;

    # get the report from the countflops.log file and clean up
    my $report = read_file("$new_path/countflops.log");
    return($report);

}



# replaces the line 
# add(new LowPassFilter(10));
# with 
# add(new LowPassFilter($targetLength));
sub set_fir_length {
    my $filename   = shift || die ("no target length");
    my $fir_length = shift || die ("no fir length");

    my $contents = read_file($filename);
    # do the find and replace
    $contents =~ s/new LowPassFilter\(1, \(3.141592653589793f \/ 3\), \d*\)/new LowPassFilter\(1, \(3.141592653589793f \/ 3\), $fir_length\)/i;
    
    # write back the modified file
    write_file($contents, $filename);
}


# Gets the number of outputs produced by one iteration of the stream steady state
sub get_output_count {
    my $path = shift || die ("no path");
    my $filename_base = shift || die ("no filename base");
    
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
