#!/usr/local/bin/perl

# simple script to make a latex file containing all the graphs 
# that test_linearity generated (eg without the matrix information, etc.)
use strict;
my $OUTPUT_FILE = "graphs-only.tex";


# open the filehandle to write
open(GFILE, ">$OUTPUT_FILE");

# write the header for our document
print GFILE make_latex_header();

# get a listing of all the files
my @files = split("\n", `ls -A workingfiles/*.ps`);
my $current_file;
foreach $current_file (@files) {
    print "working on $current_file\n";
    # add data to the latex graph file to import this figure
    print GFILE tex("\\begin{figure}\n\\center\n");    
    print GFILE tex("\\epsfxsize=6.5in\n");
    print GFILE tex("\\epsfysize=8.5\in\n");
    print GFILE ("\\epsfbox{$current_file}\n");
    print GFILE tex("\\caption{Linearity graph for $current_file}\n");
    print GFILE tex("\\end{figure}\n");
    print GFILE tex("\\clearpage\n\n");

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
    $header .= "\\begin{document}\n\n";
    return $header;
}

# footer for latex file
sub make_latex_footer {
    return "\\end{document}\n";
}

