#!/usr/local/bin/perl

# script to run the streamit compiler with linear analysis 

use strict;

my $RESULTS_DIR = "workingfiles";
my $GRAPH_FILE  = "linear_report.tex";

######## Input data ##########
my $apps_path = "/u/aalamb/streams/apps";
my $applications_path = "$apps_path/applications";
my $examples_path = "$apps_path/examples";
my $sorting_path = "$apps_path/sorts";
my $benchmark_path = "$apps_path/benchmarks";

my @files = (
	     #"FIRProgram",
	     #"SamplingRateConverter",
	     #"FilterBank",
	     #"TargetDetect",
	     #"FMRadio",
	     "BF1_1",
	     "BF1_2",
	     "BF1_3",
	     "BF1_4",
	     "BF2_1",
	     "BF2_2",
	     "BF2_3",
	     "BF2_4",
	     );

# delete the output files from any previous runs
print `rm -rf $RESULTS_DIR`;
# remake the directory
print `mkdir $RESULTS_DIR`;
# make all of the benchmark programs (convert to java)
print `make benchmarks`;

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
    my $command = ("java -Xmx1500M at.dms.kjc.Main -s " .
		   "--unroll 100000 --linearanalysis --linearreplacement --debug " .
		   "$current_file.java >& $base_filename.output");
    print "compiling $current_file\n";
    print `$command`;

    # copy the "linear.dot" file into the result directory, remove the file 
    # (so we don't get duplicates if linearanalysis dies), and make a ps file
    print `cp linear.dot $base_filename.dot`;
    print `rm linear.dot`;
    print `dot -Tps $base_filename.dot > $base_filename.ps`;
    # do the same for linear replacement
    print `cp linear-replace.dot $base_filename-replace.dot`;
    print `rm linear-replace.dot`;
    print `dot -Tps $base_filename-replace.dot > $base_filename-replace.ps`;
    
    # extract the base filename
    my @parts = split("/", $current_file);
    my $section_name = pop(@parts);

    # parse the output from the compiler into $base_filename.parsed
    print  `parse_linear_tex.pl $base_filename.output > $base_filename.parsed`;

    # read in the output and get the Linearity Report
    my $output = read_file("$base_filename.output");
    $output =~ m/Linearity Report\n(.*?\n\n)/sgi;
    my $report = $1;
    chomp($report);
    # give the report a header (like what file it refers to...)
    $report = "File: " . $current_file . "\n" . $report;
    # Write the report to the screen
    print $report;


    # Set up the section reserved for the linearity report
    print GFILE tex("\\section{Linearity Report for $section_name}\n");

    # add the data from parsing the output with the parse_lienar_tex.pl part
    print GFILE tex("\\subsection{Matrix Representations of $section_name}\n");
    print GFILE `parse_linear_tex.pl $base_filename.output`;

    # add a section with the linearity report
    print GFILE tex("\\subsection{Filter breakdown for $section_name}\n");
    print GFILE tex("\\begin{verbatim}\n");
    print GFILE tex("$report\n");
    print GFILE tex("\\end{verbatim}\n\n");
 
    # add data to the latex file to import the linearity figure
    print GFILE tex("\\subsection{Stream Graph for $section_name}\n");
    print GFILE tex("\\begin{figure}\n\\center\n");    
    print GFILE tex("\\epsfxsize=6.5in\n");
    print GFILE tex("\\epsfysize=10in\n");
    print GFILE ("\\epsfbox{$base_filename.ps}\n");
    print GFILE tex("\\caption{Linearity graph for $current_file}\n");
    print GFILE tex("\\end{figure}\n");
    print GFILE tex("\\clearpage\n\n");

    # add data to the latex file to import the linear replacement figure
    print GFILE tex("\\subsection{Linear Replacement for $section_name}\n");
    print GFILE tex("\\begin{figure}\n\\center\n");    
    print GFILE tex("\\epsfxsize=6.5in\n");
    print GFILE tex("\\epsfysize=10in\n");
    print GFILE ("\\epsfbox{$base_filename-replace.ps}\n");
    print GFILE tex("\\caption{Linear Replacement graph for $current_file}\n");
    print GFILE tex("\\end{figure}\n");
    print GFILE tex("\\clearpage\n\n");
     
    # delete the output to conserve disk space.
    #print `rm $base_filename.output`;

}

# close up the graph file
print GFILE make_latex_footer();
close(GFILE);



# format the passed in string for tex (eg convert all _ to \_)
sub tex {
    my $data = shift;
    # do _ --> \_
    $data =~ s/\_/\\\_/gi;
    return $data;
}



# header for latex file
sub make_latex_header {
    my $header = "\\documentclass{article}\n";
    $header .= "\\usepackage{fullpage}\n";
    $header .= "\\usepackage{epsfig}\n";
    $header .= "\\begin{document}\n";
    $header .= "\\tableofcontents\n";
    $header .= "\\clearpage\n\n";
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
