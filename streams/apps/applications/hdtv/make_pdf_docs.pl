#!/usr/local/bin/perl
# script converts the set of latex documents in the current 
# directory into a form that pdftex can handle. This includeds changing
# the reference names as well as converting eps images into pdf images.

use strict;

# grab a listing of the latex files
my $foo = `ls *.tex`;
my @tex_files = split("\n", $foo);
my $current_file;

foreach $current_file(@tex_files) {
    # if it is already a -pdf file, don't do anything
    if ($current_file =~ m/\-pdf/gi) {
	print "skipping $current_file\n";
	next;
    } else {
	print "Processing $current_file.\n";
    }

    # read the file into a scalar
    my $file_contents = read_file($current_file);
    # process the contents (change assorted references)
    my $new_file_contents = process_tex($file_contents);

    # figure out the new pdftex name
    my $new_name = change_extension($current_file, "-pdf", "tex");
    # write the new contents to the file
    write_file($new_file_contents, $new_name);
}


# For the passed tex source, changes all \input{*.tex} names to 
# \input{$NEW_TEX_EXTENSION} as well as changing the source for eps
# boxes to pdf and actually call esptopdf on those images
sub process_tex {
    my $tex = shift || die ("No tex passed to process_tex");
    
    # change the tex to pdftex filenames
    my @tex_files = $tex =~ m/\\input\{(.*\.tex)\}/gi;
    my $current_filename;
    foreach $current_filename (@tex_files) {
	# create the new filename
	my $new_filename = change_extension($current_filename, "-pdf", "tex");
	# use the power of perl regexps to swap the new filename for the old filename
	$tex =~ s/\\input\{$current_filename\}/\\input\{$new_filename\}/i;
    }

    # change the eps figures to pdfs
    my @eps_files = $tex =~ m/\\epsfbox\{(.*\.eps)\}/gi;
    foreach $current_filename (@eps_files) {
	# create the new filename
	my $new_filename = change_extension($current_filename, "", "pdf");
	# use the power of perl regexps to swap the new filename for the old filename
	$tex =~ s/\\epsfbox\{$current_filename\}/\\epsfbox\{$new_filename\}/i;

	# if this is in the stream graphs folder, don't actually convert(already 
	# done by make_stream_graphs)
	if ($current_filename =~ m/streamgraphs/i) {
	    print "skipping $current_filename\n";
	    next;
	}
	print "converting $current_filename to pdf\n";
	# run esptopdf to actually convert the image from eps to pdf
	print `epstopdf $current_filename`;
    }

    return $tex;
}


# returns the same filename as was passed in
# with the new extension (no period!)
sub change_extension {
    my $filename      = shift || die("no filename passed to change extension");
    my $appension     = shift;
    my $new_extension = shift || die("no new extension passed to change extension");

    # split the filename up based on the final period
    my @parts = split(/\./, $filename);
    # take off the old extension
    pop(@parts);
    # take off the final bit of the filname and add the appension to it
    push(@parts, pop(@parts) . $appension);
    # put on the new extension
    push(@parts, $new_extension);
    # return the new filename
    return join(".", @parts);
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
