#!/usr/uns/bin/perl
# library routines for reaping performance data from
# the streamit compiler.
# $Id: reaplib.pl,v 1.10 2007-06-19 06:27:19 thies Exp $

use strict;


# Parse the results that came from the simulator into the file
# filename is the basefile name where all of the results are written
# init_output_count is the number of outputs created by initiaization (this
#   many outputs are ignored).
# ss_outout_count is the number of outputs created by a steady state iteration (used
#   to create the cycles/ss iter numbers).
# usage: parse_results($filename, $btl_output, $init_output_count, $ss_output_count)
#
# returns a string of the following format:
# "total cycles until the end of the first steady state cycly:average cycles per steady state iteration"
sub parse_results {
    my $parsed_output_filename = shift || die("No output filename passed to parse_results");
    my $sim_output = shift || die("No btl output passed to parse_results");
    my $init_output_count = shift ;#|| die("No initializaton count passed to parse_results");
    my $ss_output_count = shift || die("No steady state output count passed to parse_results");

    # now, run a regexp pattern match on the raw output
    # to extract the cycle counts and the actual output from the simulator
    # (how is that for an awful regular expression?)
    my @items = $sim_output =~ m/\[(.+\:)\s(.+)]\:\s(.*)\n/g;

    # now, create a scalar that contains the data we are going to write
    my $cycle_vs_item        = "cycle\toutput item\n";
    my $cycles_per_output     = "output number\tcycles\n";
    my $ss_cycles_per_output = "ss iteration number\tcycles\n";

    my $item_count = 0;
    my $output_count = 0;     #keep track of how many outputs we have seen since last steady state iter
    my $ss_iteration_count = 0; # how many steady state iterations we have seen

    my $last_base10_cycles = 0;
    my $last_base10_ss_cycles = 0;
    while(@items) {
	my $raw_number = shift(@items); # this is the first "10:" number
	my $cycle = shift(@items);
	my $output = shift(@items);

	# conver the cycle to base 10 (from hex)
	my $base10_cycles = hex($cycle);

	# keep numbers for cycles/item graph data
	$item_count++;
	$cycle_vs_item .= "$base10_cycles\t$item_count\n";
	
	# if we are still in init, ignore data for ss counts below
	if ($item_count < $init_output_count) {
	    next;
	}
       
	
	# figure out the number of cycles that it has taken to produce the current output
	my $current_cycles_per_output = $base10_cycles - $last_base10_cycles;
	$cycles_per_output .= "$item_count\t$current_cycles_per_output\n";

	# update the output count since the last iteration
	$output_count++;
	# if we are done with an iteration, caculate the number of cycles it took per iteration
	if ($output_count == $ss_output_count) {
	    $output_count = 0;
	    $ss_iteration_count++; # increment the number of steady-state iterations we have seen
	    my $current_ss_cycles_per_output = $base10_cycles - $last_base10_ss_cycles;
	    $ss_cycles_per_output .= "$ss_iteration_count\t$current_ss_cycles_per_output\n";
	    #update the last cycle time counter
	    $last_base10_ss_cycles = $base10_cycles;
	}
	
	#update the previous cycles for next iteration
	$last_base10_cycles = $base10_cycles;	
    }

    # write the data files to disk
    write_file($cycle_vs_item, "$parsed_output_filename.cycle-vs-item");
    write_file($cycles_per_output, "$parsed_output_filename.cycles-per-output");
    write_file($ss_cycles_per_output, "$parsed_output_filename.cycles-per-ss-iteration");

    # now we nee dto do a little magic: figure out the "average number of cycles per steady state"
    # which we will do by reparsing the data we have been gathering and averaging everything but the first
    @items = split("\n", $ss_cycles_per_output);
    # chop off the first item (the header) 
    shift(@items);
    # take the first entry in lines to be the time it takes to finish the first steady state cycle
    my ($foo, $first_ss_iter_cycle_count) = split("\t", shift(@items)); 
    # now, make an average steady state cycle time
    my $running_sum = 0;
    my $total_ss_iterations = 0;
    foreach(@items) {
	# each line is cycles\tIteration
	my ($current_ss_iter, $current_cycles) = split("\t");
	$running_sum += $current_cycles;
	$total_ss_iterations++;
    }
    # assemble return string
    my $average_cycles_per_ss;
    if ($total_ss_iterations != 0) {
	$average_cycles_per_ss = $running_sum / $total_ss_iterations;
    } else {
	$average_cycles_per_ss = 0;
    }
    return "$first_ss_iter_cycle_count:$average_cycles_per_ss";
}


# writes an executive summary of the data (eg an entry in the asplos table)
# usage: write_report($filename, $options, $ss_init_count, $ss_output_count,
#                      $start_cycles, $ss_cycles, 
#                      $work_count, $bg_filename,
#                      $parsed_output_base_filename);
sub write_report {
    my $filename        = shift || die("No filename passed to write_report");
    my $options         = shift || die("No options passed to write_report");
    my $ss_init_count   = shift; # can produce 0 init outputs (normal case)
    my $ss_output_count = shift || die("No start_cycles passed to write_report");
    my $start_cycles    = shift || die("No start_cycles passed to write_report");
    my $ss_cycles       = shift || die("No steady state cycles passed to write_report");
    my $work_count      = shift || die("No steady state cycles passed to write_report");
    my $flops_count     = shift || die("No flops count passed to write_report");
    my $bg_filename     = shift || die("No blood graph file passed to write_report");
    my $base_filename   = shift || die("No basefilename passed to write_report");


    # calculate averate cycles per output
    my $average_cycles_per_output = $ss_cycles/$ss_output_count;
    # calculate throughput
    my $tput = ($ss_output_count / $ss_cycles) * 100000;
    $tput = int($tput * 100) / 100;
    # figure out percentage of useful work being performed
    my ($useful_cycles, $available_cycles) = $work_count =~ m/(.*) \/ (.*)/;
    my $utilization_percent = ($useful_cycles / $available_cycles) * 100;
    $utilization_percent = int($utilization_percent * 100) / 100;
    # figure out the mFLOPS number
    my ($foo, $actual_flops, $bar) = $flops_count =~ m/(.*?) (.*?) (.*?)/g;
    # remember that we ran the thing for 2 steady state cycles, so we need to divide by 2
    my $MFLOPS = ($actual_flops/($ss_cycles*2)) * 250;  
    $MFLOPS = int($MFLOPS * 100) / 100;
    

    my $report = "Autogenerated by reap_results.pl at: " . make_date_stamp() . "\n";
    $report .= "Filename: $filename\n";
    $report .= "Blood graph in: $bg_filename\n";
    $report .= "Compilation options: $options\n";
    $report .= "Initialization outputs: $ss_init_count\n";
    $report .= "Steady state outputs: $ss_output_count\n";
    $report .= "Cycles until the start of the second steady state iteration: $start_cycles\n";
    $report .= "Average ss cycles: $ss_cycles\n";
    $report .= "Average cycles per output: $average_cycles_per_output\n";
    $report .= "Average throughput (outputs per 10^5 cycles): $tput\n";
    $report .= "Work count: $work_count\n";
    $report .= "Utilization percent per SS cycle: $utilization_percent\n";
    $report .= "FLOPS per steady state cycle: $flops_count\n";
    $report .= "MFLOPS per steady state cycle: $MFLOPS\n";

    $report .= "\n\n\n";

    write_file($report, "$base_filename.report");
}



# Generates a summary file out of all of the reports in a given directory.
# The reports must be in the format generated by write_report
# usage generate_summary($results_directory, $summary_file);
sub generate_summary {
    my $results_directory = shift || die("no results directory passed to generate_summary");
    my $summary_file      = shift || die("no summary filename passed to generate_summary");

    # place where we are going to store the summary.
    # each element is a tab delimited list of numbers
    my @summary_table;

    # get a listing of all the report files in the result directory
    # note: each report file corresponds to a particular test with a particular set of
    #       compiler options set 
    my @files = split("\n", `ls $results_directory/*.report`);
    my $current_file;
    foreach $current_file (@files) {
	my $contents = read_file($current_file);
	# get rid of headings (eg anything upto and including the ": ")
	$contents =~ s/(.*): (.*)/$2/g;
	# parse the contents of the summary
	my ($date, $filename, 
	    $bg_filename, $options, 
	    $init_outputs, $ss_outputs,
	    $ss_2_start, $avg_ss_cycles,
	    $avg_cyc_per_out, $avg_tput,
	    $work_count, $util_pct,
	    $flops_count, $mflops_count) = split("\n", $contents);

	# add a line in the summary
	push(@summary_table,
	     "$filename\t$options\t$avg_tput\t$util_pct\t$mflops_count");
    }

    # sort the summaries by filename (first characters in the string)
    @summary_table = sort(@summary_table);

    # add heading to the table (at the beginning!)
    unshift(@summary_table,
	 "File\tOptions\tThroughput\tUtilization(percent)\tMFLOPS");
    
    # write the results out to the summary file
    write_file(join("\n", @summary_table), 
	       $summary_file);
    # write out a gratitous tex file
    write_file(make_tex_table(@summary_table),
	       "$summary_file.tex");
}





# Generates an HTML summary in the specified directory
# using the data in the summary file.
# usage generate_webpage($results_directory, $summary_file);
sub generate_webpage {
    # lots of this code was lifted from generate_summary. 
    # Will eventually replace generate_summary
    my $results_directory = shift || die("no results directory passed to generate_webpage");
    my $web_relative_dir = "webpage";


    # create a directory for dumping the webpage
    `mkdir $results_directory/$web_relative_dir`;
    `mkdir $results_directory/$web_relative_dir/images`;

    # will become the body of the webpage
    my @body_lines;

    # get a listing of all the report files in the result directory
    # note: each report file corresponds to a particular test with a particular set of
    #       compiler options set 
    my @files = split("\n", `ls $results_directory/*.report`);
    my $current_file;
    foreach $current_file (sort @files) {
	my $original_contents = read_file($current_file);
	# get rid of headings (eg anything upto and including the ": ")
	my $contents = $original_contents;
	$contents =~ s/(.*): (.*)/$2/g;
	# parse the contents of the summary
	my ($date, $filename, 
	    $bg_filename, $options, 
	    $init_outputs, $ss_outputs,
	    $ss_2_start, $avg_ss_cycles,
	    $avg_cyc_per_out, $avg_tput,
	    $work_count, $util_pct,
	    $flops_count, $mflops_count) = split("\n", $contents);

	# make a common header to stick on the generated web pages
	my $header = "<h3>$filename</h3>\n";
	$header .= "<h4>Compiled at $date with options $options<\h4><br>\n";
	
	my $base_filename = "$filename$options";
	$base_filename =~ s/ //g; # remove spaces
	# make a blood graph page
	my $bg_html = make_bloodgraph_page($results_directory, $web_relative_dir,
					   $header, $bg_filename);
	my $bg_html_filename = "$web_relative_dir/$base_filename.bloodgraph.html";
	write_file($bg_html, "$results_directory/$bg_html_filename");

	# make a dot page for the layout
	my $dot_base = "$filename$options";
	$dot_base =~ s/ //g;
	my $layout_html = make_dot_page($results_directory, $web_relative_dir,
					$header, "$dot_base.layout.dot");
	my $layout_html_filename = "$web_relative_dir/$base_filename.layout.dot.html";
	write_file($layout_html, "$results_directory/$layout_html_filename");

	# make a dot page for the stream graph
	my $stream_graph_html = make_dot_page($results_directory, $web_relative_dir,
					      $header, "$dot_base.flatgraph.dot");
	my $stream_graph_html_filename = "$web_relative_dir/$base_filename.flatgraph.dot.html";
	write_file($stream_graph_html, "$results_directory/$stream_graph_html_filename");


	# make a results summary page
	my $summary_html = make_summary_page($header, $original_contents);
	my $summary_html_filename = "$web_relative_dir/$base_filename.summary.html";
	write_file($summary_html, "$results_directory/$summary_html_filename");
	
	# make an entry in the main index page
	my $entry = "<TR>\n";
	$entry .= "   <TD>$filename</TD>\n";
	$entry .= "   <TD>$options</TD>\n";
	# put in a gratuitous thumbnail of the blood graph
	$entry .= "   <TD><a href=\"$bg_html_filename\">";
	$entry .= "<img src=\"$web_relative_dir/" . make_web_pic($results_directory, $web_relative_dir, 
								 $bg_filename, "64x48") . "\">";
	$entry .= "</a></TD>\n";
	# same thing for the stream graph
	$entry .= "   <TD><a href=\"$stream_graph_html_filename\">";
	$entry .= "<img src=\"$web_relative_dir/" . make_web_pic($results_directory, $web_relative_dir, 
								 "$results_directory/$dot_base.flatgraph.dot.ps", "64x48") . "\">";
	$entry .= "</a></TD>\n";
	# and for the layout
	$entry .= "   <TD><a href=\"$layout_html_filename\">";
	$entry .= "<img src=\"$web_relative_dir/" . make_web_pic($results_directory, $web_relative_dir, 
								 "$results_directory/$dot_base.layout.dot.ps", "64x48") . "\">";
	$entry .= "</a></TD>\n";
	$entry .= "   <TD><a href=\"$summary_html_filename\">Summary</a></TD>\n";
	$entry .= "</TR>\n";
	push(@body_lines, $entry);
	
    }

    # create the pdf file from the overall summary text file
    make_pdf_file($results_directory);

    
    # assemble the main body
    my $main_body = "<h3>Summary of results in $results_directory</h3><br>\n\n"; 
    $main_body .= "Overall Results\n";
    $main_body .= "(<a href=\"summary.txt\">text</a>)\n";
    $main_body .= "(<a href=\"summary.txt.tex\">tex</a>)\n";
    $main_body .= "(<a href=\"summary.txt.pdf\">pdf</a>)\n";
    $main_body .= "<TABLE>\n";
    $main_body .= "<TR>\n";
    $main_body .= "   <TD>Filename</TD>\n";
    $main_body .= "   <TD>Options</TD>\n";
    $main_body .= "   <TD>Blood Graph</TD>\n";
    $main_body .= "   <TD>Stream Graph</TD>\n";
    $main_body .= "   <TD>Layout</TD>\n";
    $main_body .= "   <TD>Summary</TD>\n";
    $main_body .= "</TR>\n";
    $main_body .= join("\n", sort @body_lines);
    $main_body .= "</TABLE>\n";

    # write out the index page
    my $html = make_html_page("StreamIT Compiler Performance Numbers at " . make_date_stamp(), $main_body);    
    write_file($html, "$results_directory/index.html");
}
    
# create a bloodgraph page
#usage make_bloodgraph_page($header, $bg_filename);
sub make_bloodgraph_page {
    my $results_dir  = shift || die ("no results_dir passed to make_blood_graph");
    my $web_dir      = shift || die ("no web dir passed to make_blood_graph");
    my $header       = shift || die ("no header passed to make_blood_graph");
    my $bg_filename  = shift || die ("no bloodgraph filename passed to make_blood_graph");

    # main idea: put a picture 
    
    # remove the absolute path from the bloodgraph filename to a relative path
    my @file_parts = split("/", $bg_filename);
    my $web_bg_filename = "../" . pop(@file_parts);

    # make a thumbnail
    my $big_thumb_web_filename = make_web_pic($results_dir, $web_dir, $bg_filename, "640x480");
    
    # set up the body 
    my $body  = $header . "\n\n" . "<a href=\"$web_bg_filename\"><img src=\"$big_thumb_web_filename\"></a>\n";
    # return the html
    return make_html_page("Bloodgraph", $body);
}

# create a summary page from the contents of a .report file
# usage make_summary_page($header, $report_contents);
sub make_summary_page {
    my $header           = shift || die ("no header passed to make_summary_page");
    my $report_contents  = shift || die ("no report_header passed to make_summary_page");

    my $body = "$header";
    
    # basic strategy is to make table with each of the lines of the report in its own table line
    $body .= "<TABLE>\n";
    my @report_lines = split("\n", $report_contents);
    my $current_report_line;
    foreach $current_report_line (@report_lines) {
	my ($prop, $val) = split(": ", $current_report_line);
	$body .= "<TR><TD>$prop</TD><TD>$val</TD></TR>\n";
    }
    $body .= "</TABLE>";

    return make_html_page("Summary page", $body);
}


# create a dot page
#usage make_dot_page($results_dir, $web_dir, $header, $dot_filename);
sub make_dot_page {
    my $results_dir  = shift || die ("no results_dir passed to make_dot_page");
    my $web_dir      = shift || die ("no web dir passed to make_dot_page");
    my $header       = shift || die ("no header passed to make_dot_page");
    my $dot_filename = shift || die ("no dot filename passed to make_dot_page");
    
    # remove the absolute path from the bloodgraph filename to a relative path
    my @file_parts = split("/", $dot_filename);
    my $base_filename = pop(@file_parts);
    my $web_dot_filename = "../" . $base_filename;

    # make a ps of the dot file (that we will then convert)
    `/u/diego/bin/dot -Tps $results_dir/$dot_filename > $results_dir/$dot_filename.ps`;
    
    # make a gif of the file
    my $web_filename = "images/$dot_filename.gif";
    `convert -geometry 640x480 $results_dir/$dot_filename.ps $results_dir/$web_dir/$web_filename`;

    
    # set up the body 
    my $body  = $header . "\n\n" . "<a href=\"../$base_filename\"><img src=\"$web_filename\"></a>\n";
    # return the html
    return make_html_page("Dot File", $body);
}

#
# Makes a PDF file out of the summary.txt.tex file in the specified results directory
# Usage: make_pdf_file($results_dir)
sub make_pdf_file {
    my $results_dir = shift || die ("No results dir passed to make_pdf_file");
    print "executing pdflatex...";
    `/usr/bin/pdflatex $results_dir/summary.txt.tex`;
    print `mv summary.txt.pdf $results_dir/summary.txt.pdf`;
    print "done.\n";
}

# makes and scales an image to the specified dimensions
# (passed as a param to convert) and returns the filename (relative) 
# of the image
# usage: make_web_pic($results_dir, $web_dir, $filename, $dimensions)
sub make_web_pic {
    my $results_dir = shift || die ("No results dir passed to make_blood_thumbnail");
    my $web_dir     = shift || die ("No web dir passed to make_blood_thumbnail");
    my $bg_filename = shift || die ("No blood graph filename passed to make_blood_thumbnail");
    my $dimensions  = shift || die ("No dimensions passed to make_blood_thumbnail");

    # figure out what the web relative filename of the image will be
    my @file_parts = split("/", $bg_filename);
    my $web_bg_filename = "images/" . pop(@file_parts) . "$dimensions.gif";
    
    # now, use convert to make the image the specified dimensions
    my $command = "convert -geometry $dimensions! $bg_filename $results_dir/$web_dir/$web_bg_filename";
    #print "Command: $command\n\n";
    `$command`;
    
    return $web_bg_filename;
}

    

# sticks in the appropriate tags to make a common look for pages
# around title and body
# usage: make_html_page($title, $body);
sub make_html_page {
    my $title = shift || die("No title passed to make_html_page");
    my $body = shift || die("No title passed to make_html_page");

    my $webpage = "<HTML>\n";
    $webpage .= "<HEAD><TITLE>$title</TITLE></HEAD>\n";
    $webpage .= "<BODY>\n";
    $webpage .= "$body<br>\n";
    $webpage .= "<i>auto generated at: " . make_date_stamp() . "<br>\n";
    $webpage .= "<a href=\"mailto:aalamb\@cag.lcs.mit.edu\">aalamb\@cag.lcs.mit.edu</a>\n";
    $webpage .= "</BODY>\n";
    $webpage .= "</HTML>";

    return $webpage;
}

###############################################
############## Utility Subroutines#############
###############################################

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


# Makes a tex table out of the passed array
# which contains tab separated values. First element 
# contains the titles for the table.
# usage: make_tex_table(@data)
sub make_tex_table {
    my @data = @_;
    

    # set up doc preamble
    my $tex = "\\documentclass{article}\n";
    $tex.= "\\usepackage{lscape}\n";
    $tex .= "\\begin{document}\n";
    $tex .= "\\begin{landscape}\n";

    # set up table header
    my $header = shift(@data); # grab first row (headings)
    my @headings = split("\t",$header);
    $tex .= "\\begin{tabular} {";
    foreach (@headings) {
	$tex .= "l|";
    }
    chop($tex);
    $tex .= "}\n";
    $tex .= join(" & ", @headings) . " \\\\\n";
    $tex .= "\\hline\n";
    
    # now, make the body of the table
    my $current_data_line;
    foreach $current_data_line (@data) {
	my @items = split("\t", $current_data_line);
	$tex .= join(" & ", @items) . " \\\\\n";
    }

    # end the table
    $tex .= "\\end{tabular}\n";

    $tex .= "\\end{landscape}\n";
    $tex .= "\\end{document}\n";

    # finally, replace _ with \_ because tex sux
    $tex =~ s/_/\\_/g;
    
    return $tex;
}
    








# wacky perl syntax (included files have to return true...)
1;
