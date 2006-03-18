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

# *** Available PAPI preset events ***
# not all of these can be used at the same time
# you can try a combination, if it can't be done
# you will get a PAPI conflict error
#
# PAPI_L1_DCM   Level 1 data cache misses
# PAPI_L1_ICM   Level 1 instruction cache misses
# PAPI_L2_TCM   Level 2 cache misses
# PAPI_TLB_DM   Data translation lookaside buffer misses
# PAPI_TLB_IM   Instruction translation lookaside buffer misses
# PAPI_TLB_TL   Total translation lookaside buffer misses
# PAPI_L1_LDM   Level 1 load misses
# PAPI_L2_LDM   Level 2 load misses
# PAPI_L2_STM   Level 2 store misses
# PAPI_BR_TKN   Conditional branch instructions taken
# PAPI_BR_NTK   Conditional branch instructions not taken
# PAPI_BR_MSP   Conditional branch instructions mispredicted
# PAPI_BR_PRC   Conditional branch instructions correctly predicted
# PAPI_TOT_IIS  Instructions issued
# PAPI_TOT_INS  Instructions completed
# PAPI_FP_INS   Floating point instructions
# PAPI_LD_INS   Load instructions
# PAPI_SR_INS   Store instructions
# PAPI_BR_INS   Branch instructions
# PAPI_VEC_INS  Vector/SIMD instructions
# PAPI_RES_STL  Cycles stalled on any resource
# PAPI_TOT_CYC  Total cycles
# PAPI_LST_INS  Load/store instructions completed
# PAPI_L1_ICA   Level 1 instruction cache accesses
# PAPI_FP_OPS   Floating point operations

hpcrun -e PAPI_TOT_CYC -e PAPI_TOT_INS -e PAPI_FP_INS -e PAPI_L1_DCM -e PAPI_L1_ICM -e PAPI_L2_TCM -w ${BENCHMARK}.profile-output -- ${BENCHMARK_AND_ARGS}
if test "$?" != 0; then echo "Exiting: non-zero exit code";exit 1;fi

hpcprof -t ${BENCHMARK} ${BENCHMARK}.profile-output
if test "$?" != 0; then echo "Exiting: non-zero exit code";exit 1;fi

rm -rf ${BENCHMARK}.profile-output