#!/bin/csh
# AAL 6/25/2002 Script that runs regression tests every evening
# (gets called from cron job on cagfram-46.lcs.mit.edu user
# aalamb.
# $Id: run_reg_tests.sh,v 1.4 2002-07-01 19:18:08 aalamb Exp $

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

# file where error messages are printed
setenv REG_ERR /u/aalamb/streams/regtest/regtest_errors.txt

# temporary file
setenv TEMP /tmp/regtest.temp

# log file for the all results
setenv REG_LOG /u/aalamb/streams/regtest/regtest_log.txt



#delete old error file
rm -rf $REG_ERR

# (date/time stamp for this regression test run)
echo "**********" >& $REG_LOG
echo "Starting Regression Test Run" >>& $REG_LOG
/bin/date >>& $REG_LOG
echo "**********" >>& $REG_LOG

# get the latest copy from 
cd $STREAMIT_HOME
cvs update >>& $REG_LOG
cd compiler
cd kopi
./compile >>& $REG_LOG

# run the makefile which executes the regression test
/usr/local/bin/make -C $STREAMIT_HOME/regtest >>& $REG_LOG
# touch error file so cat won't complain
touch $REG_ERR



# collect all output and send it to andrew
echo "Regression Output:" >> $TEMP;
cat $REG_LOG >> $TEMP
echo "\n\nError Messages:" >> $TEMP;
cat $REG_ERR >> $TEMP
echo "\n\nResults: " >>$TEMP;

# send mail to with the results of the test
cat $TEMP | mail -s "Full StreamIT Regression Test Results" aalamb@mit.edu



# create a summary message that contains performance numbers
$STREAMIT_HOME/regtest/tools/parse_results.pl $STREAMIT_HOME/regtest/regtest_log.txt $STREAMIT_HOME/regtest/regtest_errors.txt $STREAMIT_HOME/regtest/regtest_results.txt > $TEMP

# send mail to with the results of the test
cat $TEMP | mail -s "Full StreamIT Regression Test Summary" aalamb@mit.edu


# create an executive message that doesn't contain performance numbers
$STREAMIT_HOME/regtest/tools/parse_results.pl $STREAMIT_HOME/regtest/regtest_log.txt $STREAMIT_HOME/regtest/regtest_errors.txt > $TEMP

# send mail to with the results of the test to the streamit mailing list
cat $TEMP | mail -s "StreamIT Regression Test Summary" aalamb@mit.edu commit-stream@cag.lcs.mit.edu



# clean up temp file
rm -rf $TEMP
