#!/usr/local/bin/perl

# This file contains the core routines for gathering numbers
# for the PLDI benchmarks.

use strict;

# the streamit frontend, the streamit compiler, the C compiler and dynamorio
my $STREAMIT_FRONTEND    = "java streamit.frontend.ToJava --full";
my $STREAMIT_COMPILER    = "strc";
my $STREAMIT_GCC         = ("gcc -O2 " .
			    "-I" . $ENV{"STREAMIT_HOME"} . "/library/c " .
			    "-I" . $ENV{"ATLAS_HOME"} . "/include " .
			    "-L" . $ENV{"STREAMIT_HOME"} . "/library/c " .
			    "-L" . $ENV{"ATLAS_HOME"} . "/lib/Linux_P4SSE2 ");
my $STREAMIT_GCC_POSTFIX = "-lcblas -latlas -lstreamit -lsrfftw -lsfftw -lm";
my $STREAMIT_DYNAMORIO   = "dynamorio";

# the program to use to compare output
my $CMP = "/u/aalamb/streams/regtest/tools/compare_uni/pl";
# the number of iterations to run the program for
my $NUM_ITERS = 10000;



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


#########
# subroutine to do a compile test, and parse results.
# Return value is (outputs, flops, fadds, fmuls).
# do_test($path, $base_filename, $options, $descr, $iterations)
#########
sub do_test {
    my $path = shift || die ("no flops");
    my $base_filename = shift || die ("no base");
    my $options = shift || die("no options");
    my $descr = shift || die ("no description");
    my $iters = shift || $NUM_ITERS;
    # compile with specified options
    print "$descr:";
    do_compile($path, $base_filename, $options);
    
    # figure out how many outputs are produced
    my $outputs = get_output_count($path, $base_filename, $iters);
    
    # run the dynamo rio test and get back the results
    my $report = run_rio($path, $base_filename, $iters);
    
    # extract the flops, fadds and fmul count from the report
    my ($flops) =  $report =~ m/saw (.*) flops/;
    my ($fadds) =  $report =~ m/saw (.*) fadds/;
    my ($fmuls) =  $report =~ m/saw (.*) fmuls/;

    return ($outputs, $flops, $fadds, $fmuls);
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
    do_streamit_compile($new_path, $filename_base, $options);
    # compile the C code to generate an executable
    do_c_compile($new_path, $filename_base);
}

########
# Subroutine to use the streamit frontend. Generates $filename.java from $filename.str
# usage: do_frontend_compile($path, $filename);
########
sub do_frontend_compile {
    my $new_path      = shift || die ("no path passed to do_frontend_compile.");
    my $filename_base = shift || die ("no filename passed to do_frontend_compile.");

    # run streamit compiler to generate C code.
    print "(str->java)";
    `cd $new_path; $STREAMIT_FRONTEND $filename_base.str > $filename_base.java`;
}

########
# Subroutine to use the streamit compiler on the specified file
# usage: do_streamit_compile($path, $filename, $compiler_options);
########
sub do_streamit_compile {
    my $new_path      = shift || die ("no path passed to do_streamit_compile.");
    my $filename_base = shift || die ("no filename passed to do_streamit_compile.");
    my $options       = shift;

    # run streamit compiler to generate C code.
    print "(java->c)";
    `cd $new_path; $STREAMIT_COMPILER $options $filename_base.java >& $filename_base.c`;
}

########
# Subroutine to use the c compiler on a streamit compiler generated file
# usage: do_c_compile($path, $filename, $compiler_options);
########
sub do_c_compile {
    my $new_path      = shift || die ("no path passed to do_c_compile.");
    my $filename_base = shift || die ("no filename passed to do_c_compile.");

    # compile the C code to generate an executable
    print "(c->exe)";
    `cd $new_path; $STREAMIT_GCC $filename_base.c -o $filename_base.exe $STREAMIT_GCC_POSTFIX`;
}



#######
# Subroutine to execute the program with dynamo
sub run_rio {
    my $new_path      = shift || die ("no new path passed");
    my $filename_base = shift || die ("no filename base passed");
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
    my $num_iters = shift || $NUM_ITERS;

    print "(output count)";
    
    # run the program for one iter piping its output to wc
    # whose output we will parse and return a value.
    my $wc_results = `$path/$filename_base.exe -i $num_iters | wc -l`;
    my ($count) = $wc_results =~ m/(\d*)\n/gi;
    return $count;
}

# get the actual N of the output.
# (N is a parameter that is calculated by the compiler when doing 
# the frequency replacement optimization).
sub get_N {
    my $path = shift || die ("no path");
    my $filename_base = shift || die ("no filename base");
    # read in the c file and extract the information from the comments.
    my $contents = read_file("$path/$filename_base.c");
    my ($N) = $contents =~ m/N=(\d*)/gi;
    return $N;
}

# gets a date/time stamp based on the output from 'date'
# usage get_date_time_stamp
sub get_date_time_stamp {
    my $stamp = `date`;
    chomp $stamp; # remove trailing new line.
    # replace colons and spaces
    $stamp =~ s/\s/\_/gi; # " " --> "_"
    $stamp =~ s/\:/\-/gi; # ":" --> "-"

    return $stamp;
}

# Time the execution of a program using "time"
# returns time for execution
# usage: time_execution($path, $program[, $num_iters]
sub time_execution {
    my $path = shift || die ("no path");
    my $filename_base = shift || die ("no filename");
    my $num_iters = shift || $NUM_ITERS;

    # print out status
    print "(time $num_iters)";
    
    #temp file
    my $TEMP_FILE = "timing_output" . rand() . ".txt";
    # crazy redirect hack to get access to the output of the "time" command
    `/usr/local/bin/bash -c \"time $path/$filename_base.exe -i $num_iters\" >/dev/null  2>$TEMP_FILE`;
    my $time_result = read_file($TEMP_FILE);
    `rm $TEMP_FILE`;

    my ($real_m, $real_s) = $time_result =~ m/real\s*(\d*)m([\d\.]*)s/g;
    my ($user_m, $user_s) = $time_result =~ m/user\s*(\d*)m([\d\.]*)s/g;
    my ($sys_m,  $sys_s)  = $time_result =~ m/sys\s*(\d*)m([\d\.]*)s/g;
    # calculate the overall execution time (using the "real" numbers)
    my $execution_time = $real_m*60+$real_s;
    
    # return time and load averages.
    return($execution_time);
}

# returns the load averages as reported by uptime
# returns load averages for (1,5,15) minutes.
sub get_load {
    # "uptime" to get the work average numbers
    my $uptime_result = `uptime`;
    # pull out the numbers
    my ($load_1, $load_5, $load_15) = $uptime_result =~ m/load average: ([\d\.]*), ([\d\.]*), ([\d\.]*)/g;
    # return those bad boys
    return ($load_1, $load_5, $load_15);
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

# writes a list of lines of a tsv file
# into the actual file, saves a copy
# of the results in the "opt_results" directory
# and sends andrew an email with the contents.
# usage save_tsv($filename, $subject, @lines)
sub save_tsv {
    my $filename = shift || die ("no filename passed to save_tsv");
    my $subject = shift; # the subject of the email message that we are going to send.
    my @lines = @_;

    print "(writing $filename)";
    open (RFILE, ">$filename");
    print RFILE join("\n", @lines);
    close RFILE;

    # now, save a copy of the tsv file to the results diectory
    my $save_dir = "opt_results";
    `mkdir -p $save_dir`;
    my $date_stamp = get_date_time_stamp();
    # pull out just the name from the full filename
    my @parts = split("/", $filename);
    my $name = pop(@parts);
    print "(saving backup)";
    open (RFILE, ">$save_dir/$date_stamp".$name);
    print RFILE join("\n", @lines);
    close RFILE;

    # finally, send the email
    print "(sending mail)";
    open (MHMAIL, "|mhmail aalamb\@mit.edu -s \"$subject\"");
    print MHMAIL "auto sent by reaplib at " . `date`;
    print MHMAIL join("\n", @lines); 
    close(MHMAIL);
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

# replaces the line 
# add(new LowPassFilter(10));
# with 
# add(new LowPassFilter($targetLength));
sub set_fir_length {
    my $filename   = shift || die ("no target length");
    my $fir_length = shift || die ("no fir length");

    my $contents = read_file($filename);
    # do the find and replace
    $contents =~ s/new .*LowPassFilter\(1, \(3.141592653589793f \/ 3\), \d*\)/new LowPassFilter\(1, \(3.141592653589793f \/ 3\), $fir_length\)/i;
    
    # write back the modified file
    write_file($contents, $filename);
}

# replaces all occurences of printf with //printf in the specified file
sub remove_prints {
    my $path = shift || die ("no path");
    my $base_filename = shift || die ("no base");
    print "(-print)";
    # read in the c file
    my $contents = read_file("$path/$base_filename.c");
    # replace printf with //printf
    $contents =~ s/(printf.*)/\/\/$1\nPOP_DEFAULTB\(float\);/g;
    # write the changes back to disk
    write_file($contents, "$path/$base_filename.c");
}


# wacked perl snytax
1;
