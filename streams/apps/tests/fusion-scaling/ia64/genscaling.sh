#!/bin/bash
# 1 = compile, 2 = run
MODE=2
if [ "$MODE" = "2" ]
    then
echo "echo '*************************************************' "
echo "echo '*** You must be on an ITANIUM for this to run ***' "
echo "echo '*************************************************' "
echo "sleep 1"
fi
# determine host name
echo rm -f address1 address
echo "nslookup \`hostname\` 1>address1 2>/dev/null"
echo "cat address1 | grep Address | grep -v \# > address"
echo "perl -pi -e 's/Address:\ //g' address"
echo "perl -pi -e 's/\n//g' address"
echo export LD_LIBRARY_PATH=/home/linux/encap/gcc-3.4.3/lib/
echo export STREAMIT_HOME=/u/thies/research/streams/streams
echo . $STREAMIT_HOME/include/dot-bashrc
echo rm -f results.csv
echo "echo 'Peek,MODULATION,,,,,,,,COPY-SHIFT,,,,,,,,COPY-SHIFT + PEEK-SCALING,,,,,,,,COPY-SHIFT + SCALAR REPLACE,,,,,,,,COPY-SHIFT + SCALAR-REPLACE + PEEK-SCALING' > results.csv"
echo "echo ',Outputs,Runtime1,,Runtime2,,Runtime3,,Outputs,Runtime1,,Runtime2,,Runtime3,,Outputs,Runtime1,,Runtime2,,Runtime3,,Outputs,Runtime1,,Runtime2,,Runtime3,,Outputs,Runtime1,,Runtime2,,Runtime3,' >> results.csv"
echo "echo ',,min,sec,min,sec,min,sec,,min,sec,min,sec,min,sec,,min,sec,min,sec,min,sec,,min,sec,min,sec,min,sec,,min,sec,min,sec,min,sec' >> results.csv"
if [ "$MODE" = "1" ]
    then
echo rm -rf fusion*
fi
if [ "$MODE" = "2" ]
    then
# setup itanium 
echo . /opt/intel/compiler70/ia64/bin/eccvars.sh
echo cd $STREAMIT_HOME/library/cluster
echo make -f Makefile ia64
echo cd -
fi
for i in 1 `seq 8 8 128`;
do
  echo
  echo "# $i"
  if [ "$MODE" = "2" ]
      then 
  echo "echo -n '$i,' >> results.csv"
  fi
  if [ "$MODE" = "1" ]
      then
  echo cp ../Scaling.str Scaling.str
  echo perl -pi -e \'s/E\\ =\\ 64/E\\ =\\ $i/g\' Scaling.str
  fi

  for j in `seq 2 6`;
  do
    if [ "$MODE" = "1" ]
	then
    if [ "$j" = "1" ]
	then
        # NO FUSION
	echo strc -u 128 Scaling.str
	# run with no fusion -- TODO
    fi
    if [ "$j" = "2" ]
	then
        # MODULATION
	echo strc -cluster 1 -u 128 -fusion -standalone -modfusion Scaling.str
    fi    
    if [ "$j" = "3" ]
	then
        # COPY-SHIFT
	echo strc -cluster 1 -u 128 -fusion -standalone Scaling.str
    fi    
    if [ "$j" = "4" ]
	then
        # COPY-SHIFT + PEEK-SCALING
	echo strc -cluster 1 -u 128 -fusion -standalone -peekratio 4 Scaling.str
    fi    
    if [ "$j" = "5" ]
	then
        # COPY-SHIFT + SCALAR-REPLACE
	echo strc -cluster 1 -u 128 -fusion -standalone -destroyfieldarray Scaling.str
    fi    
    if [ "$j" = "6" ]
	then
        # COPY-SHIFT + SCALAR-REPLACE + PEEK-SCALING
	echo strc -cluster 1 -u 128 -fusion -standalone -destroyfieldarray -peekratio 4 Scaling.str
    fi
    # do cluster-config
    echo "echo -n '0 ' > cluster-config.txt"
    echo cat address >> cluster-config.txt
    echo make -f Makefile.cluster clean
    echo make -f Makefile.cluster fusion
    # have to put things in directories so we can build ia64 binaries on ia64
    echo cp fusion fusion-count-$i-$j
    fi
    # calculate iterations to try to keep runtime constant
    calibrate_iters=$(echo "10000/(($i+3)/3)" | bc)
    run_iters=$(echo "50000*$calibrate_iters" | bc)
    if [ "$MODE" = "2" ]
	then
    # count outputs
    echo "sh -c './fusion-count-$i-$j -i $calibrate_iters | wc -l > outputs' 2> /dev/null"
    echo perl -pi -e \'s/ //g\' outputs
    echo perl -pi -e \'s/\\n/,/g\' outputs
    echo "cat outputs >> results.csv"
    fi
    if [ "$MODE" = "1" ]
	then
    # eliminate print
    echo perl -pi -e \'s/\\\{\\\{/\\\{\\\{volatile\\ int\\ v\\\;/g\' thread0.cpp
    echo perl -pi -e \'s/printf\\\(\\\"\\\%d\\\\\n\\\", result/v \\\=\\ \\\(result/g\' thread0.cpp
    echo make -f Makefile.cluster clean
    # need its own directory so we can build binary on actual itanium
    echo rm -rf fusion-$i-$j
    echo mkdir fusion-$i-$j
    echo "cp * fusion-$i-$j"
    fi
    if [ "$MODE" = "2" ]
      then
    echo cd fusion-$i-$j
    echo make -f Makefile.cluster fusion_ia64
    # gather 3 runtimes
    for k in `seq 1 3`;
      do
      echo "sh -c 'time ./fusion_ia64 -i $run_iters' 2> thetime1"
      echo "grep user thetime1 | grep -v sys > thetime"
      echo perl -pi -e 's/\\t//g' thetime
      echo perl -pi -e \'s/\\ //g\' thetime
      echo perl -pi -e \'s/user//g\' thetime
      echo perl -pi -e \'s/\\n//g\' thetime
      echo perl -pi -e \'s/m/,/g\' thetime
      echo perl -pi -e \'s/s/,/g\' thetime
      echo "cat thetime >> ../results.csv"
    done
    echo cd ..
    fi
  done
  # new line
  echo "echo >> results.csv"
done
# cleanup stray commas
echo "perl -pi -e 's/,\\n/\\n/g' results.csv"
