#!/bin/csh
# AAL 6/25/2002 Script that runs regression tests every evening
# (gets called from cron job on cagfram-46.lcs.mit.edu user
# aalamb).
# $Id: run_reg_tests.sh,v 1.7 2002-07-15 21:42:48 aalamb Exp $

# Environmental Variables
# home directory for CVS
setenv CVSROOT /projects/raw/cvsroot
setenv STREAMIT_HOME /u/aalamb/streams/
# path for RAW tools
setenv TOPDIR /home/bits6/aalamb/starsearch
setenv CLASSPATH .:/usr/local/jdk1.3/jre/lib/rt.jar:$STREAMIT_HOME/compiler/kopi/3rdparty/JFlex/lib:$STREAMIT_HOME/compiler/kopi/3rdparty/getopt:$STREAMIT_HOME/compiler/kopi/classes:$STREAMIT_HOME/apps/libraries:$STREAMIT_HOME/misc/java:$STREAMIT_HOME/scheduler/v1/java:/usr/uns/java/antlr-2.7.1:$STREAMIT_HOME/compiler/frontend:$STREAMIT_HOME/scheduler/v2/java

# the script to use for 

# set up automatic testing so that the text tester gets used
setenv AUTOMATIC_TEST true

# temporary file
setenv TEMP /tmp/regtest.temp
# directory where the log files should be stored
setenv LOG_DIR $STREAMIT_HOME/regtest/logs/

# log file for the all results
setenv REG_LOG $STREAMIT_HOME/regtest/regtest_log.txt
# file where error messages are printed
setenv REG_ERR $STREAMIT_HOME/regtest/regtest_errors.txt
# file where success messages are printed
setenv SUCCESS $STREAMIT_HOME/regtest/regtest_success.txt
# file where performance messages are stored
setenv RESULTS $STREAMIT_HOME/regtest/regtest_results.txt



#delete old error file
rm -rf $REG_ERR


######### Do the acutal regression test run ####################
# (date/time stamp for this regression test run)
echo "**********" >& $REG_LOG
echo "Starting Regression Test Run" >>& $REG_LOG
/bin/date >>& $REG_LOG
echo "**********" >>& $REG_LOG
# get the latest copy from 
cd $STREAMIT_HOME
cvs update -dP >>& $REG_LOG
cd compiler
cd kopi
./compile >>& $REG_LOG
# run the makefile which executes the regression test
/usr/local/bin/make -C $STREAMIT_HOME/regtest >>& $REG_LOG
# (date/time stamp the end of the run)
echo "**********" >>& $REG_LOG
echo "Regression Test Run Done" >>& $REG_LOG
/bin/date >>& $REG_LOG
echo "**********" >>& $REG_LOG
# touch error file so cat won't complain
touch $REG_ERR


############# Assemble the Logfile 
# collect all output and send it to andrew
echo "Regression Run Output:" > $TEMP;
cat $REG_LOG >> $TEMP
echo "\n\n####################\nError Messages:" >> $TEMP;
cat $REG_ERR >> $TEMP
# send the log file as a mail message to Andrew
cat $TEMP | mail -s "Full StreamIT Regression Test Output" aalamb@mit.edu
# copy the log file to standard 
setenv LOG_FILENAME `$STREAMIT_HOME/regtest/tools/make_daily_log.pl $TEMP $LOG_DIR `




# create a summary message that contains performance numbers
$STREAMIT_HOME/regtest/tools/parse_results.pl $REG_LOG $REG_ERR $SUCCESS $RESULTS > $TEMP

# send mail to with the results of the test to andrew
cat $TEMP | mail -s "StreamIT Regression Test Summary (with performance)" aalamb@mit.edu


# create an executive message that doesn't contain performance numbers
echo "Logfile: \n" > $TEMP
echo $LOG_FILENAME >> $TEMP
echo "\n\n" >> $TEMP
$STREAMIT_HOME/regtest/tools/parse_results.pl $REG_LOG $REG_ERR $SUCCESS >> $TEMP

# send mail to with the results of the test to the streamit mailing list
cat $TEMP | mail -s "StreamIT Regression Test Summary" aalamb@mit.edu commit-stream@cag.lcs.mit.edu nmani@cag.lcs.mit.edu
#cat $TEMP | mail -s "StreamIT Regression Test Summary" aalamb@mit.edu 

# clean up temp file
rm -rf $TEMP
