#!/usr/local/bin/perl

# script to run the streamit compiler with linear analysis 

use strict;

my $RESULTS_DIR = "workingfiles";
my $REPORT_FILE = "report.txt";
my $GRAPH_FILE  = "graphs.tex";


######## Input data ##########
my $apps_path = "/u/aalamb/streams/apps";
my $applications_path = "$apps_path/applications";
my $examples_path = "$apps_path/examples";
my $sorting_path = "$apps_path/sorts";
my $benchmark_path = "$apps_path/benchmarks";

my @files = (
	     
	     #"$benchmark_path/beamformer/streamit/BeamFormer.java",
	     "$benchmark_path/bitonic-sort/streamit/BitonicSort.java",
	     # has some sort of problem with the lowering phase -- non constant
	     # something...
	     #"$benchmark_path/bitonic-sort/streamit/BitonicSort_inlined.java",
	     
	     # Runs out of memory with 512 M of ram....
	     "$benchmark_path/bitonic-sort/streamit/BitonicSortRecursive.java",
	     
	     # seems to work fine now
	     "$benchmark_path/fft/streamit/*.java",

	     # had an issue in checkRep that I fixed.
	     "$benchmark_path/fir/streamit/FIRfine.java",

	     
	     "$benchmark_path/fm/streamit/LinkedFMTest.java",

	     "$benchmark_path/gsm/streamit/Gsm.java",
	     "$benchmark_path/nokia/streamit/Linkeddcalc.java",
	     "$benchmark_path/vocoder/streamit/*.java",
	     "$benchmark_path/beamformer/streamit/BeamFormer.java",
	 

	     "$applications_path/crc/CrcEncoder32Test.java",
	     "$applications_path/DCT/DCT.java",
	     "$applications_path/nokia-fine/Linkeddcalc.java",


	     
	     
	     
	     "$examples_path/autocor/AutoCor.java",
	     "$examples_path/fft/FFT.java",
	     "$examples_path/file/FileTest.java",
	     "$examples_path/lattice/Lattice.java",
	     "$examples_path/fib/Fib.java",
	     "$examples_path/fib/Fib2.java",
	     "$examples_path/hello/HelloWorld6.java",
	     "$examples_path/matrixmult/MatrixMult.java",
	     "$examples_path/matrixmult/MatrixMultBlock.java",
	     "$examples_path/mergesort/MergeSort.java",
	     
	     # needs conditional operation support.
	     #"$examples_path/mergesort/MergeSort16.java",
	     
	     "$examples_path/vectadd/VectAdd.java",	     
	     "$examples_path/vectadd/VectAdd1.java",	     

	     
	     );

# delete the output files from any previous runs
`rm -rf $RESULTS_DIR`;
# remake the directory
`mkdir $RESULTS_DIR`;
# remove the old report file
`rf -f $REPORT_FILE`;

# open the file handle for writing reports to
open(RFILE, ">$REPORT_FILE");
# write the TSV header
print RFILE "file(s)\tLinear Filters/Total Filters (% linear)\n";

# open the graph file and write a header to it.
open(GFILE, ">$GRAPH_FILE");
print GFILE make_latex_header();

# iterate over all input files
my $current_file;
foreach $current_file (@files) {
    # figure out a base filename for the results directory.
    my $base_filename = $current_file;
    # remove slashes
    $base_filename =~ s/\///gi;
    # convert spaces --> _
    $base_filename =~ s/\s/_/gi;
    # convert stars (*) --> .
    $base_filename =~ s/\*/\./gi;
    # add on the results path
    $base_filename = $RESULTS_DIR . "/" . $base_filename;

    # run the compiler and save its output to $base_filename.output
    my $command = ("java -Xmx1500M at.dms.kjc.Main -s --constprop --linearanalysis --debug " .
			    "$current_file >& $base_filename.output");
    `$command`;

    # copy the "linear.dot" file into the result directory
    print `cp linear.dot $base_filename.dot`;
    # use dot to make a ps file
    print `dot -Tps $base_filename.dot > $base_filename.ps`;
    # convert ps to eps
    print `ps2epsi $base_filename.ps $base_filename.eps`;

    # add data to the latex graph file to import this figure
    print GFILE "\\begin{figure}\n\\center\n";    
    print GFILE "\\epsfxsize=4.5in\n";
    print GFILE "\\epsfbox{$base_filename.eps}\n";
    print GFILE "\\caption{Linearity graph for $current_file}\n";
    print GFILE "\\end{figure}\n\n";
    
    

    # parse the output from the compiler into $base_filename.parsed
    $command = "parse_linear_output.pl $base_filename.output > $base_filename.parsed";
    `$command`;

    # read in the output and get the Linearity Report
    my $output = read_file("$base_filename.output");
    #print "output: " . $output;
    $output =~ m/Linearity Report\n(.*?\n\n)/sgi;
    my $report = $1;
    chomp($report);

    # give the report a header (like what file it refers to...)
    $report = "File: " . $current_file . "\n" . $report;

    # Write the report to the screen
    print $report;

    # munge the report and write a line to our report tsv file
    $report =~ m/Filters:(\s*)(.*)/;
    print RFILE "$current_file\t$2\n";


}

# close up the report file
close(RFILE);
# close up the graph file
print GFILE make_latex_footer();
close(GFILE);


# header for latex file
sub make_latex_header {
    my $header = "\\documentclass{article}\n";
    $header .= "\\usepackage{fullpage}\n";
    $header .= "\\usepackage{epsfig}\n";
    $header .= "\\begin{document}\n\n";
    return $header;
}

# footer for latex file
sub make_latex_footer {
    return "\\end{document}\n";
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
