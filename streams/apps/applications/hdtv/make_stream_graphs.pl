#!/usr/local/bin/perl
# creates stream graphs using the files in the streamgraph directory
#

use strict;

my @files = ("SGTrellisEncoder",
	     "SGTrellisDecoder",
	     "SGTrellisEncoderPipeline",
	     "SGTrellisDecoderPipeline",
	     "SGRSEncoder",
	     "SGRSDecoder",
	     "SGConvolutionalInterleaver",
	     "SGConvolutionalDeinterleaver",);

# compile the stream graph class files
`jikes streamgraphs/*.java`;


# go into the streamgraphs directory
`cd streamgraphs`;
# for each file that we want make
my $current_file;
foreach $current_file (@files) {
    # set up the java command to make a dot file
    my $java_command = ("java -classpath " . 
			$ENV{"CLASSPATH"} . ":streamgraphs " .
			"$current_file -norun -printgraph > $current_file.dot");
    # set up a command to convert the dot file to postscript
    my $dot_command = "dot -Tps $current_file.dot > $current_file.ps";
    # set up the command to convert the ps file to eps
    my $convert_command = "ps2epsi $current_file.ps $current_file.eps";
    # command to cleanup the current directory, and put the files into the streamgraph directory
    my $cleanup_command = "mv $current_file.eps streamgraphs; rm $current_file.ps $current_file.dot";

    # now, execute them
    print "processing $current_file:";
    print "(java)"    . `$java_command`;
    print "(dot)"     . `$dot_command`;
    print "(ps2epsi)" . `$convert_command`;
    print "(clean)"   . `$cleanup_command`;
    print "done.\n";
}

# finish up by going back to the original directory
    `cd ..`;
