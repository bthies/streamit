#!/bin/sh
# AAL 6/25/2002 Script that runs results gatherer every evening
# (gets called from cron job on cagfram-46.lcs.mit.edu user
# aalamb).
# $Id: run_results.sh,v 1.10 2002-10-21 20:41:06 aalamb Exp $

echo CVSROOT: ${CVSROOT:=/projects/raw/cvsroot} >/dev/null
echo STREAMIT_HOME: ${STREAMIT_HOME:=$HOME/streams} >/dev/null
echo TOPDIR: ${TOPDIR:=/home/bits6/$LOGNAME/starsearch} >/dev/null
CLASSPATH=.:/usr/local/jdk1.3/jre/lib/rt.jar:$STREAMIT_HOME/compiler/kopi/3rdparty/JFlex/lib:$STREAMIT_HOME/compiler/kopi/3rdparty/getopt:$STREAMIT_HOME/compiler/kopi/classes:$STREAMIT_HOME/apps/libraries:$STREAMIT_HOME/misc/java:$STREAMIT_HOME/scheduler/v1/java:/usr/uns/java/antlr-2.7.1:$STREAMIT_HOME/compiler/frontend:$STREAMIT_HOME/scheduler/v2/java

echo LOGFILE: ${LOGFILE:=$STREAMIT_HOME/regtest/tools/results_log.txt} >/dev/null
# file that gets generated automatically by the regression test framework
echo RESULT_SCRIPT: ${RESULT_SCRIPT:=$STREAMIT_HOME/regtest/regtest_perf_script.txt} >/dev/null

export CVSROOT STREAMIT_HOME TOPDIR LOGFILE RESULT_SCRIPT

echo "-------------" > $LOGFILE
echo "Input file:"   >> $LOGFILE
cat $RESULT_SCRIPT   >> $LOGFILE
echo "-------------" >> $LOGFILE

echo "-------------" >> $LOGFILE
echo "Starting..."   >> $LOGFILE
date                 >> $LOGFILE
echo "-------------" >> $LOGFILE

# simply run the results script (with the results we want for asplos)
/usr/local/bin/perl -I$STREAMIT_HOME/regtest/tools/ $STREAMIT_HOME/regtest/tools/reap_results.pl  $RESULT_SCRIPT >> $LOGFILE 2>&1

echo "-------------" >> $LOGFILE
echo "Done..."       >> $LOGFILE
date                 >> $LOGFILE
echo "-------------" >> $LOGFILE


# mail results to andrew
cat $LOGFILE | mail -s "Numbers generated" $LOGNAME@cag.lcs.mit.edu

# remove the log file
rm -rf $LOGFILE
