#!/bin/bash

NUM=1

env NAME=$NAME PROGDIR=$PROGDIR NODES=$NODES qsub ccp.sh

while [[ $NUM -le $NODES ]]; do
    env RANK=$NUM NAME=$NAME PROGDIR=$PROGDIR ITERS=$ITERS qsub node.sh
    ((NUM++))
done