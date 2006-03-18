#!/bin/sh

HPCTOOLKIT=/home/streamit/3rdparty/hpctk
export PATH="${HPCTOOLKIT}/bin:${PATH}"

# Assume arguments valid
VALID_ARGS=1;

if [ $# -ge 1 ]; then
   BENCHMARK="$1";
   BENCHMARK_AND_ARGS="$*";
else
   VALID_ARGS=0;
fi

if [ $VALID_ARGS -eq 0 ]; then
    echo '> Usage: hwprof.sh <benchmark> [benchmark args]';
    exit 1;
fi;

hpcrun -e PAPI_TOT_CYC -e PAPI_TOT_INS -e PAPI_FP_INS -e PAPI_L1_LDM -e PAPI_L1_ICM -w ${BENCHMARK}.profile-output -- ${BENCHMARK_AND_ARGS}
if test "$?" != 0; then echo "Exiting: non-zero exit code";exit 1;fi

hpcprof -t ${BENCHMARK} ${BENCHMARK}.profile-output
if test "$?" != 0; then echo "Exiting: non-zero exit code";exit 1;fi

rm -rf ${BENCHMARK}.profile-output