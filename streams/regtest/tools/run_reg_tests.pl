#!/usr/local/bin/perl
# AAL 6/25/2002 Script that runs regression tests every evening
# This is a modern adaptation of the venerable run_reg_tests.sh rewritten
# in perl because I don't know how to use all of the crazy unix command
# line utilities necessary to do this stuff.
#
# $Id: run_reg_tests.pl,v 1.5 2002-12-10 15:23:28 aalamb Exp $

use strict;

# debug flag -- if this flag is set, only the admins are emailed results 
# and the test-bed command is run. 1 = true, 0= false
my $DEBUG = 0;

# admin email addresses where we want the crazy emails to go (space separated)
my $ADMINS = "aalamb\@mit.edu";
# user email addresses who want to get regtest-results
my $USERS = "commit-stream\@cag.lcs.mit.edu nmani\@cag.lcs.mit.edu";

# Set up necessary environmental variables so that programs will run
# automatic testing so that the text tester gets used all of the time.
$ENV{"AUTOMATIC_TEST"}="true";
# path for RAW tools
$ENV{"TOPDIR"}="/home/bits6/NO_BACKUP/streamit/starsearch";


# Root location to store the reg test working files
my $REGTEST_ROOT = "/home/bits6/NO_BACKUP/streamit/regtest_working";

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
$ENV{"PATH"} = "/projects/raw/current/rawcc/compiler/bin:/usr/ccs/bin:/u/diego/bin/:$streamit_home:/usr/local/bin:/usr/uns/bin:/usr/bin/X11:/usr/ucb:/bin:/usr/bin:/usr/etc:/etc:/usr/games:";
my $class_path = ".:/usr/local/jdk1.3/jre/lib/rt.jar:$streamit_home/compiler/kopi/3rdparty/JFlex/lib:$streamit_home/compiler/kopi/3rdparty/getopt:$streamit_home/compiler/kopi/classes/:$streamit_home/apps/libraries:$streamit_home/misc/java:$streamit_home/scheduler/:/usr/uns/java/antlr-2.7.1/:$streamit_home/compiler/frontend:$streamit_home/compiler/kopi/3rdparty/cplex/cplex.jar";
$ENV{"CLASSPATH"} = $class_path;
$ENV{"CLASSROOT"} = "$streamit_home/compiler/kopi/classes";


# try and compile the freshly checked out streams directory
# note that this requires our rediculous build process of make/clean/compile three times
print MHMAIL saved_execute("cd $working_dir/streams/compiler/kopi; make");
print MHMAIL saved_execute("cd $working_dir/streams/compiler/kopi; ./clean");
print MHMAIL saved_execute("cd $working_dir/streams/compiler/kopi; ./compile");
print MHMAIL saved_execute("cd $working_dir/streams/compiler/kopi; make");
print MHMAIL saved_execute("cd $working_dir/streams/compiler/kopi; ./clean");
print MHMAIL saved_execute("cd $working_dir/streams/compiler/kopi; ./compile");

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
    `/usr/local/bin/make -C $streamit_home/regtest test-all >& $REG_LOG`;
}

## (date/time stamp the end of the run)
`echo \"Regression Test Run Done\" >> $REG_LOG`;
`/bin/date >> $REG_LOG`;
`touch $REG_ERR`;

# set the results to the admins
`cat $REG_LOG | mhmail $ADMINS -s \"Streamit Regression Test Log\"`; 
# set the errors to the admins
`cat $REG_ERR | mhmail $ADMINS -s \"Streamit Regression Test Errors\"`; 


# open the mhmail program to print the execuative summary to
if ($DEBUG) {
    open(MHMAIL, "|mhmail $ADMINS -s \"Streamit Regression Test Summary\"");
} else {
    open(MHMAIL, "|mhmail $USERS -s \"Streamit Regression Test Summary\"");
}
# make a note in the email about where the working files are
print MHMAIL "(Working directory: $streamit_home)\n";

my $email_body = saved_execute("$streamit_home/regtest/tools/parse_results.pl $REG_LOG $REG_ERR $SUCCESS");
# remove the full paths from the email (to make it easier to read)
$email_body =~ s/$streamit_home//gi;
print MHMAIL $email_body;

print MHMAIL "\n-----------------\n";

# write the email body to a file so that we can run the "which apps are in regtest" script
write_file($email_body, "$streamit_home/parsed_results.txt");
# figure out what applications are in and out of the regtest
my $apps_executed = saved_execute("/bin/sh $streamit_home/regtest/tools/apps_directory.sh $streamit_home/parsed_results.txt");
# remove the full path (and leave only the relative paths) (so it is easier to see what apps are missing)
$apps_executed =~ s/$streamit_home//gi;
print MHMAIL $apps_executed;

#send the email.
close(MHMAIL);





# returns a clean time date stamp without any spaces or
# other invalid filename characters
sub get_clean_timedate_stamp {
    # call the date command
    my $timedate = `date`;
    chomp($timedate);
    # remove all bad stuff (like spaces, and colons)
    $timedate =~ s/ /_/gi;
    $timedate =~ s/\:/_/gi;
    return $timedate;
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
