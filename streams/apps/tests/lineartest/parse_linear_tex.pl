#!/usr/local/bin/perl

# parse the output of compiler run with linear analysis and verboose mode turned on,
# and extract the matrix/vector pairs that correspond to each linear filter. 
# Then create tex mat layout code for displaying the matricies nicely. 
#
# usage: parse_linear_tex.pl output_trace_from_compiler.txt
#
# Note: This code was taken from parse_lienar_matrix.pl and so the code base forked
# from there. I am not worried as that script seems to be doing just fine.

use strict;

# Maximum number of columns to print
my $MAX_COLS = 16;

# main entry point
main();

sub main {
    my $filename = shift(@ARGV) || die("usage: parse_linear_tex.pl filename");

    # read in the contents to a scalar
    my $output_contents = read_file($filename);

    my @filter_contents;
    my @pipeline_contents;

    # do a pattern match to extract the matrix/vector pairs for filters
    @filter_contents = $output_contents =~ m/Linear filter found: .*?name=(.*?) (.*?)\n-->Matrix:\n(.*?)-->Constant Vector:\n(.*?]]\n)/sig;

    @pipeline_contents = $output_contents =~ m/Linear pipeline found: .*?name=(.*?) (.*?)\n-->Matrix:\n(.*?)-->Constant Vector:\n(.*?]]\n)/sig;

    # combine the set of contents together
    my @contents = (@filter_contents, @pipeline_contents);

    # now, make a hash map with name mapping to matrix data \t vector data
    my %data;
    while(@contents) {
	my $name = shift(@contents);
	my $foo= shift(@contents);
	my $matrix=shift(@contents); 
        chomp($matrix);
	my $vector = shift(@contents);
	chomp($vector);
	$data{"$name"} = $matrix . "\t" . $vector;
    }

    # now, print the data in the hash in a nice tex form
    print_nice_tex(\%data);

}




# prints out key\nvalue pairs for the contents of the hash
# in a nice tex math environment.
sub print_nice_tex {
    print_tex_header();
    my $hashref = shift || die ("no hash ref passed to print_hash_ref");
    my $current_key;
    foreach $current_key (sort keys %$hashref) {
	# get the data from the hash to work with
	my $current_data = $$hashref{$current_key};
	# split on tab
	my ($matrix, $vector) = split("\t", $current_data);
	# the data line looks like 
	# [[data data data]
	#  [data data data]
	#  [data data data]]
	# do a pattern match to extract the rows from the matrix
	my @matrix_rows = $matrix =~ m/\[(\[.*\])\]/sgi;
	my $matrix_row = " " . shift(@matrix_rows);
	# break down the rows separately
	@matrix_rows = $matrix_row =~ m/\[(.*?)\]/sgi;

	# same thing to extract the row from the vector
	my @vector_rows = $vector =~ m/\[\[(.*)\]\]/sgi;

	my $current_title = $current_key;
	# replace all underscores with \_
	$current_title =~ s/\_/\\_/gi;
	print "\\textbf{$current_title} (\$xA+b=y\$)\n";
	print "\\[  A=";
	print_matrix(\@matrix_rows);
	print "\\] \\[  b=";
	print_matrix(\@vector_rows);
	print "\\]  \n\n";
	
    }
    print_tex_footer();
}

# print out a matrix whose rows are contained in the passed listref
#\begin{array}{ccc}
#a & b & c \\
#d & e & f \\
#g & h & i 
sub print_matrix {
    my $list_ref = shift || die ("no list ref passed");

    # use the first row to figure out how many columns there are
    my @first_row_elems = split(" ", $$list_ref[0]);
    
    

    print "\\left[ \n";
    print "\\begin{array}{";

    # if there are more than $MAX_COLS cols, print out a placeholder instead
    if (@first_row_elems > $MAX_COLS) {
	print "c}\n";
	print "TooBig";
    } else {
	# print out the format command (eg the number of columns) c's
	my $i;
	for ($i=0; $i<@first_row_elems; $i++) {print "c";}
	print "}\n"; # end of the setup for the matrix
	
	# now, process the matrix rows one at a time
	my $current_row;
	foreach $current_row (@$list_ref) {
	    chomp($current_row); # remove ending newline
	    my @elems = split(" ", $current_row);
	    my @new_elems;
	    my $current_elem;
	    # now, the idea is to remove all zero elements
	    foreach $current_elem (@elems) {
		my ($real_part, $imag_part) = split(/\+/, $current_elem);
		chop($imag_part); # remove the i
		# clean up the numbers by removing extra decimal points, etc.
		$real_part = clean_num($real_part);
		$imag_part = clean_num($imag_part);
		my $new_num;
		if (($real_part eq "") and ($imag_part eq "")) {
		    $new_num = "0";
		} elsif ($real_part eq "") {
		    $new_num = $imag_part . "j";
		} elsif ($imag_part eq "") {
		    $new_num = $real_part;
		} else {
		    $new_num = "$real_part+$imag_part" . "j";
		}
		push(@new_elems, $new_num);
	    }
	    
	    # replace the spaces with ampersands (col boundaries in tex)
	    my $row = join(" \& ", @new_elems);
	    # append on a row delim and newline (\\)
	    $row .= " \\\\\n";
	    print $row;
	}
    }

    # now, end the matrix
    print "\\end{array}\n";
    print "\\right] \n";
}

# clean up the number a bit. Returns "" if the number is 0
sub clean_num {
    my $num = shift || die ("no number passed");

    $num = sprintf("%.2f", $num);

    # split up number on decimal point
    my ($whole_part, $dec_part) = split(/\./, $num);
    # if the dec part is zero, get rid of it.
    if ($whole_part eq "0") {$whole_part = "";}
    if ($dec_part eq "0") {$dec_part = "";}
    if ($dec_part eq "00") {$dec_part = "";}

    if (($whole_part eq "") and ($dec_part eq "")) {
	return "";
    } elsif ($whole_part eq "") {
	return ".$dec_part";
    } elsif ($dec_part eq "") {
	return $whole_part;
    } else {
	return "$whole_part.$dec_part";
    }
}

# Print the appropriate TEX preamble commands
sub print_tex_header {
    #print "\\documentclass{article}\n";
    #print "\\usepackage{fullpage}\n";
    #print "\\begin{document}\n\n";

}

# print the appropriate TEX footer commands
sub print_tex_footer {

    #print "\\end{document}\n";
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

