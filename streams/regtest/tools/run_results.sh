#!/bin/csh
# AAL 6/25/2002 Script that runs results gatherer every evening
# (gets called from cron job on cagfram-46.lcs.mit.edu user
# aalamb).
# $Id: run_results.sh,v 1.3 2002-07-17 18:35:52 aalamb Exp $

setenv LOGFILE /u/aalamb/streams/regtest/tools/results_log.txt
# file that gets generated automatically by the regression test framework
setenv RESULT_SCRIPT /u/aalamb/streams/regtest/regtest_perf_script.txt


setenv CVSROOT /projects/raw/cvsroot
setenv STREAMIT_HOME /u/aalamb/streams/
setenv TOPDIR /home/bits6/aalamb/starsearch
setenv CLASSPATH .:/usr/local/jdk1.3/jre/lib/rt.jar:$STREAMIT_HOME/compiler/kopi/3rdparty/JFlex/lib:$STREAMIT_HOME/compiler/kopi/3rdparty/getopt:$STREAMIT_HOME/compiler/kopi/classes:$STREAMIT_HOME/apps/libraries:$STREAMIT_HOME/misc/java:$STREAMIT_HOME/scheduler/v1/java:/usr/uns/java/antlr-2.7.1:$STREAMIT_HOME/compiler/frontend:$STREAMIT_HOME/scheduler/v2/java

echo "-------------" > $LOGFILE
echo "Starting..."  >> $LOGFILE
date >> $LOGFILE
echo "-------------" >> $LOGFILE

# simply run the results script (with the results we want for asplos)
cd /u/aalamb/streams/regtest/tools/
reap_results.pl $RESULT_SCRIPT >>& $LOGFILE

echo "-------------" >> $LOGFILE
echo "Done..."  >> $LOGFILE
date >> $LOGFILE
echo "-------------" >> $LOGFILE





# mail results to andrew
cat $LOGFILE | mail -s "Numbers generated" aalamb@mit.edu

# remove the log file
#rm -rf $LOGFILE
