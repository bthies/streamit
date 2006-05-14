#!/bin/bash

#SBP -q @server

#PBS -N node
#PBS -l ncpus=2
#PBS -j oe
#PBS -o /u/qmahoney/streams/library/cluster/tester/.tmp/node.out
#PBS -r n
#PBS -V

# This must be an absolute path
DIR=/u/qmahoney/streams/library/cluster/tester/.tmp/

# Quit after 10 seconds
START=`date +%s`
START=$START+10
while [[ ( ! -e $DIR/.ccphost ) && ( `date +%s` -lt $START ) ]]; do
    sleep 2
done
if [[ ! -e $DIR/.ccphost ]]; then
    echo "Error, CCP not running!"
    exit 1
fi

# Find CCP
CCP=`cat $DIR/.ccphost`

# Print header
echo This is node $RANK at $HOSTNAME > $DIR/output-$RANK
echo CCP is at $CCP >> $DIR/output-$RANK
echo Name is $NAME >> $DIR/output-$RANK
echo Program is in $PROGDIR >> $DIR/output-$RANK
echo "-------------------------" >> $DIR/output-$RANK
echo >> $DIR/output-$RANK

# Actually run the server

cd $PROGDIR
exec run_cluster -ccp $CCP -i $ITERS >> $DIR/output-$RANK 2>> $DIR/output-$RANK