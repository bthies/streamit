#!/bin/bash

#PBS -N ccp
#PBS -l ncpus=2
#PBS -j oe
#PBS -o /u/qmahoney/streams/library/cluster/tester/.tmp/ccp.out
#PBS -r n
#PBS -V

DIR=/u/qmahoney/streams/library/cluster/tester/.tmp/

#echo o-host $PBS_O_HOST
#echo jobid $PBS_JOBID
#echo jobname $PBS_JOBNAME

echo $HOSTNAME > $DIR/.ccphost

RANK=ccp

# Print header
echo This is node $RANK at $HOSTNAME > $DIR/output-$RANK
echo Name is $NAME >> $DIR/output-$RANK
echo Program is in $PROGDIR >> $DIR/output-$RANK
echo "-------------------------" >> $DIR/output-$RANK
echo >> $DIR/output-$RANK

cd $PROGDIR
exec run_cluster -runccp $NODES >> $DIR/output-ccp 2>> $DIR/output-ccp