#! /usr/uns/bin/perl
#
###############################################################################
# This script takes a file containing a list of .str files (with pathnames)
# It creates a unique directory per .str file and puts into that directory
# a file 'script' that can be used to compile and run the file in 
# number-gathering mode under PBS.
#
# It also takes a second parameter: list of swtiches to the compiler.
# The switches must be quoted if there are any spaces or characters subject
# to shell substitution in the list.  The switch "-raw 4" and "-N $num_iters" 
# is assumed and should not be passed. If no other switches are passed, use ""
#
# This is tailored to the structure of the apps/benchmarks/asplos06 directory:
# There is a builtin assumption that the .str files are in directories named
# 'streamit'. that they refer to input and output files by relative location
# (so the directories built by this script must be parallel to the streamit
# directory).  Any .str files not in a directory named 'streamit' are ignored.
# For each FILE.str file, this script creates a directory FILE.rawSWITCHES
# parallel to the streamit directory, where SWITCHES is the passed list of
# switches with any spaces removed.
#
# An input file to run this script on all str in subdirectories of the cwd
# may be created with find as follows: 
# find . -name "*.str" -exec echo `pwd`/'{}' \; > files
# $STREAMIT_HOME/apps/benchmarks/asplos06/scripts/CreateExecutionScripts.perl \
#   files "-spacetime -slicethresh 100"
# To submit all of the resulting script files to PBS, for running on fast
# processors:  do something like:
# find . -name "script" -exec qsub -l nodes=1:dual2.2GHz {} \;
# If you have created many scripts and only want to submit for certain switches
# try something like:
# find . -name "*slicethresh100" -exec  qsub -l nodes=1:dual2.2GHz '{}'/script \;
# I recomend the fast processors because they have 2GB of main memory which
# allows our 1.7GB heaps for java to be used without swapping.  The PBS
# pragmas in the generated script causes it to be put on the short queue.
# For runs expected to exceed 12 hours, you should edit this.
#
# Comments on the generated script files:
# (1) Number gathering for the old raw requires prints rather than FileWriters
#  $STREAMIT_HOME/misc/scripts/turnOffPrints.pl can take care of this for you.
# (2) The number of steady state iterations used is 10, except for tde where
#  it is 1. 
# (3) If you are generating bloodgraphs, uncomment the line that compresses
#  the bloodgraphs for you.  The bloodgraph.ppm files are very large but
#  very compressible, We have seen a factor of 3000 compression from .ppm to
#  .gif.bz2 !
# (4) Intermediate files are not cleaned up, but are left in the generated
#  subdirectories, except for compressing bloodgraphs.  stderr is rerouted
#  to file stderr in the directory containing the script. stdout is rerouted
#  to file stdout  in the directory containing the script.
###############################################################################

use strict;
use warnings;

my $fileslist = shift;
my $args = shift;
my $argsext = $args;
$argsext =~ s/ //g;


local @ARGV = ($fileslist);

foreach (<>) { 
    s/\/\.\//\//;    # /./ replaced with /
    my ($head, $tail) = /^(.+)\/streamit\/([-_A-Za-z0-9]+)\.str/;
    next unless $2;
    # $head is directory name up to '/streamit/'
    # $tail is file name without '.str' suffix
    my $numIters = 3;
    my $rawside = 4;
    $numIters = 3 if $tail =~ /^tde.*/;
    my $emailAddress = 'mgordon@cag.csail.mit.edu';
    my $dirName = "$head/$tail.raw$argsext";
    system("mkdir", "$dirName") unless -e $dirName;
    (my $script = <<"EOT") =~ s/^\s+//gm;
       #!/bin/sh
       #PBS -S /bin/sh
       #PBS -k n
       #PBS -N $tail
       #PBS -q long
       #PBS -v STREAMIT_HOME
       ##PBS -M $emailAddress
       ##PBS -m a
       ## mailed info is relatively useless, just mailing if aborted.

       cd $dirName
       #record the job id and the host name for debugging
       #qsub ./\${runName}.sh > \${tail}.pbsid
       echo \$HOSTNAME > $tail.hostname
       exec >stdout 2>stderr
       . \$STREAMIT_HOME/include/dot-bashrc
       PATH=\${PATH}:/usr/local/bin:/usr/uns/bin:/usr/bin:/bin
       cp ../streamit/$tail.str .
       nice strc -raw $rawside -N $numIters $args $tail.str
       nice make -f Makefile.streamit run
       #check for correctness with library, appends to results.out if correct
       \$STREAMIT_HOME/apps/benchmarks/asplos06/scripts/CheckCorrectness.perl $tail.str $numIters
       #convert bloodgraph.ppm bloodgraph.gif && rm bloodgraph.ppm && bzip2 bloodgraph.gif
       
EOT
    my $scriptName = "$dirName/script";
    open (SCRIPTFILE, ">$scriptName") or die "Can't open $head.raw/script: $!";
    print SCRIPTFILE "$script";
    close (SCRIPTFILE);
    chmod (0755,$scriptName) or die "Can't chmod $scriptName: $!";
}

