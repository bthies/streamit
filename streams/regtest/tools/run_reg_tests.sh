#!/bin/csh
# AAL 6/25/2002 Script that runs regression tests every evening
# (gets called from cron job on cagfram-46.lcs.mit.edu user
# aalamb.
# $Id: run_reg_tests.sh,v 1.1 2002-06-25 20:03:04 aalamb Exp $

# Vars so that java compiles correctly
setenv CLASSPATH .:/usr/local/jdk1.3/jre/lib/rt.jar:/u/aalamb/streams//compiler/kopi/3rdparty/JFlex/lib:/u/aalamb/streams//compiler/kopi/3rdparty/getopt:/u/aalamb/streams//compiler/kopi/classes:/u/aalamb/streams//apps/libraries:/u/aalamb/streams//misc/java:/u/aalamb/streams//scheduler/v1/java:/usr/uns/java/antlr-2.7.1:/u/aalamb/streams//compiler/frontend:/u/aalamb/streams//scheduler/v2/java
setenv STREAMIT_HOME /u/aalamb/streams/



# file where error messages are printed
setenv REG_ERR /u/aalamb/streams/regtest/regtest_errors.txt

# temporary file
setenv TEMP /tmp/regtest.temp

# file where we will direct the output of running the current commands
setenv REG_OUT /u/aalamb/streams/regtest/cron_results_current.txt

# log file for the all results
setenv REG_LOG /u/aalamb/streams/regtest/cron_results_log.txt


#delete old error file
rm -rf $REG_ERR
# run the makefile which executes the regression test
/usr/local/bin/make -C /u/aalamb/streams/regtest >& $REG_OUT
# touch error file so cat won't complain
touch $REG_ERR

# assemble the body of the mail message
echo "Streamit Regression Test Results" > $TEMP
date >> $TEMP;
echo "Regression Output:" >> $TEMP;
cat $REG_OUT >> $TEMP
echo "\n\nError Messages:" >> $TEMP;
cat $REG_ERR >> $TEMP


# send mail to with the results of the test
cat $TEMP | mail -s "StreamIT Regression Test Results" aalamb@mit.edu


# add an entry to the logfile with all of the results

# (date/time stamp for this regression test run)
echo "**********" >>& $REG_OUT
echo "Starting Regression Test Run" >>& $REG_OUT
/bin/date >>& $REG_OUT
echo "**********" >>& $REG_OUT
cat $TEMP >> $REG_OUT

# clean up temp file
rm -rf $TEMP




