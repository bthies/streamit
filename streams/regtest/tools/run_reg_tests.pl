#!/usr/local/bin/perl -w
# AAL 6/25/2002 Script that runs regression tests every evening
# This is a modern adaptation of the venerable run_reg_tests.sh rewritten
# in perl because I don't know how to use all of the crazy unix command
# line utilities necessary to do this stuff.
#
# Usage: run_reg_test.pl -- runs all of the regtests  (eg make test-all)
#        run_reg_test.pl nightly -- runs nightly regtests (eg make test-nightly)
#
# $Id: run_reg_tests.pl,v 1.18 2003-07-07 14:09:32 dmaze Exp $

use strict;
use POSIX qw(strftime);

# debug flag -- if this flag is set, only the admins are emailed results 
# and the test-bed command is run. 1 = true, 0= false
my $DEBUG = 0;

# admin email addresses where we want the crazy emails to go (space separated)
my $ADMINS = "streamit-regtest-log\@cag.lcs.mit.edu";
# user email addresses who want to get regtest-results
my $USERS = "streamit-regtest\@cag.lcs.mit.edu nmani\@cag.lcs.mit.edu";

# Set up necessary environmental variables so that programs will run
# automatic testing so that the text tester gets used all of the time.
$ENV{"AUTOMATIC_TEST"}="true";
# path for RAW tools
$ENV{"TOPDIR"}="/home/bits7/NO_BACKUP/streamit/starsearch";
# Root location to store the reg test working files
my $REGTEST_ROOT = "/home/bits7/NO_BACKUP/streamit/regtest_working";
# Root location to store RT output
my $RT_ROOT = "/projects/streamit/www/rt";


# Start the actual work. If the first command line arg is "nightly" 
# then set the nightly flag
my $CVS_TARGET = "test-all";
if (@ARGV[0] eq "nightly") {
    $CVS_TARGET = "test-nightly";
}

# Get a time/date stamp without spaces,etc. for the directory name
my $working_dir = "$REGTEST_ROOT/". get_clean_timedate_stamp();


# open a pipe to mhmail to send mail (print all of our results there).
$SIG{PIPE} = sub {die "caught sigpipe from mhmail";}; # to handle mhmail explosions
open(MHMAIL, "|mhmail $ADMINS -s \"Streamit Regression Test Log (checkout and compile)\"");
print MHMAIL "The following are the results of running the regtest: \n";

# make the working directory
print MHMAIL saved_execute("mkdir $working_dir");
print MHMAIL "working directory $working_dir created\n";
# checkout the streams directory
print MHMAIL saved_execute("cd $working_dir; cvs -d /projects/raw/cvsroot co streams");

# Set up the compilation path
my $streamit_home = "$working_dir/streams";
$ENV{"STREAMIT_HOME"} = "$streamit_home/";
$ENV{"PATH"} = "/projects/raw/current/rawcc/compiler/bin:/usr/ccs/bin:$streamit_home:/usr/local/bin:/usr/uns/bin:/usr/bin/X11:/bin:/usr/bin";
my $class_path = ".:/usr/uns/jdk1.3.1_01/jre/lib/rt.jar:/usr/uns/java/antlr.jar:$streamit_home/compiler:$streamit_home/compiler/3rdparty:$streamit_home/compiler/3rdparty/cplex/cplex.jar:$streamit_home/library/java:$streamit_home/eclipse";
$ENV{"CLASSPATH"} = $class_path;
$ENV{"CLASSROOT"} = "$streamit_home/compiler/kopi/classes";


# try and compile the freshly checked out streams directory
print MHMAIL saved_execute("make -C $working_dir/streams/compiler");

# now that we have compiled the compiler, set up the various files where we will direct the output
# temporary file use to generate body of the messages
my $TEMP = "/tmp/regtest.temp";
# directory where the log files should be stored
my $LOG_DIR = "$streamit_home/regtest/logs/";
# log file for the all results
my $REG_LOG = "$streamit_home/regtest/regtest_log.txt";
# file where error messages are printed
my $REG_ERR = "$streamit_home/regtest/regtest_errors.txt";
# file where success messages are printed
my $SUCCESS = "$streamit_home/regtest/regtest_success.txt";
# file where performance messages are stored
my $RESULTS = "$streamit_home/regtest/regtest_results.txt";

#delete old error file
print MHMAIL saved_execute("rm -rf $REG_ERR");

# close the mhmail program to send the mail to 
close(MHMAIL);

# remove old success and error logs
`rm -rf $SUCCESS`;
`rm -rf $REG_ERR`;

######### Do the acutal regression test run ####################
## (date/time stamp the start of the run)
`echo \"Regression test started:\" > $REG_LOG`;
`/bin/date >> $REG_LOG`;

## run the makefile which executes the regression test
if ($DEBUG) {
    `/usr/local/bin/make -C $streamit_home/regtest test-bed >& $REG_LOG`;
} else {
    `/usr/local/bin/make -C $streamit_home/regtest $CVS_TARGET >& $REG_LOG`;
}

## (date/time stamp the end of the run)
`echo \"Regression Test Run Done\" >> $REG_LOG`;
`/bin/date >> $REG_LOG`;
`touch $REG_ERR`;

# set the results to the admins
`cat $REG_LOG | mhmail $ADMINS -s \"Streamit Regression Test Log ($CVS_TARGET)\"`; 
# set the errors to the admins
`cat $REG_ERR | mhmail $ADMINS -s \"Streamit Regression Test Errors ($CVS_TARGET)\"`; 


# open the mhmail program to print the execuative summary to
if ($DEBUG) {
    open(MHMAIL, "|mhmail $ADMINS -s \"Streamit Regression Test Summary ($CVS_TARGET)\"");
} else {
    open(MHMAIL, "|mhmail $USERS -s \"Streamit Regression Test Summary ($CVS_TARGET)\"");
}
# make a note in the email about where the working files are
print MHMAIL "(Working directory: $streamit_home)\n";

my $email_body = saved_execute("$streamit_home/regtest/tools/parse_results.pl $REG_LOG $REG_ERR $SUCCESS");
# write the email body to a file so that we can run the "which apps are in regtest" script
write_file($email_body, "$streamit_home/parsed_results.txt");
# remove the full paths from the email (to make it easier to read)
$email_body =~ s/$streamit_home//gi;
print MHMAIL $email_body;

print MHMAIL "\n-----------------\n";

# figure out what applications are in and out of the regtest
my $apps_executed = saved_execute("/bin/sh $streamit_home/regtest/tools/apps_directory.sh $streamit_home/parsed_results.txt");
# remove the full path (and leave only the relative paths) (so it is easier to see what apps are missing)
$apps_executed =~ s/$streamit_home//gi;
print MHMAIL $apps_executed;

#send the email.
close(MHMAIL);

# generate RT-compatible output too
open(MHMAIL, "|mhmail $ADMINS -s \"Output from html_execute.pl\"");
print MHMAIL saved_execute("$streamit_home/regtest/tools/html_results.pl $streamit_home/ $REG_LOG $REG_ERR $SUCCESS $RT_ROOT/Summary $RT_ROOT/listing.html");
close (MHMAIL);



# returns a clean time date stamp without any spaces or
# other invalid filename characters
sub get_clean_timedate_stamp {
    return strftime "%Y%m%d.%H%M%S.%a", localtime;
}

# executes a command and returns both the stdout and stderr in the same
# string (scalar)
sub saved_execute {
    my $command = shift || die "no command passed to saved_execute";
    # find an unused temp file
    my $temp_number = 0;
    my $temp_file = "/tmp/commandResults$temp_number";
    while (-e $temp_file) {
	$temp_number++;
	$temp_file = "/tmp/commandResults$temp_number";
    }
    # execute the command, sending both stdin and stderr to a temp file
    `$command >& $temp_file`;
    # return the contents of the temp file (and remove it)
    my $contents = read_file($temp_file);
    $contents .= `rm $temp_file`;
    return $contents;
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
