#!/bin/bash

# Check for correct usage
if [ $# -ne 4 ]; then
    echo "usage:"
    echo -e "    ./Worker.sh tilera_size compiler_options benchmark aggregate_file"
    exit 1
fi

numproc=${1}

options=${2#\'}
options=${options%\'}

benchmark=${3}
benchmark_dir=`dirname $benchmark`
benchmark_file=${benchmark##/*/}
benchmark_name=${benchmark_file%.str}

aggregate_file=${4}

# Check environment variables
if [ -z "$STREAMIT_HOME" ]; then
    echo '$STREAMIT_HOME must be defined'
    exit 1
fi

if [ -z "$TILERA_HOME" ]; then
    echo '$TILERA_HOME must be defined'
    exit 1
fi

if [ -z "$JAVA_HOME" ]; then
    echo '$JAVA_HOME must be defined'
    exit 1
fi

echo "Host: $HOSTNAME"
echo "\$STREAMIT_HOME: ${STREAMIT_HOME}"
echo "\$TILERA_HOME: ${TILERA_HOME}"
echo "\$JAVA_HOME: ${JAVA_HOME}"
echo "\$PATH: ${PATH}"
echo

# Check that benchmark exists
if [ ! -f ${benchmark} ]; then
    echo "Benchmark \"${benchmark}\" does not exist"
    exit 1
fi

# Copy benchmark to current directory
cp ${benchmark} .
#cp -rf ${benchmark_dir}/* .

# Compile benchmark and execute
echo "${STREAMIT_HOME}/strc -t $numproc -N 10 $options $benchmark_file"
echo "===="

${STREAMIT_HOME}/strc -t $numproc -N 10 $options $benchmark_file

echo
echo "make"
echo "===="

make | tee make.txt
#echo "Average cycles per SS for 10 iterations: 16169374, avg cycles per output: 12030" > make.txt

# Extract results, place into aggregate results file
if [ ! -z ${aggregate_file} ]; then
    echo "${benchmark_name}_${options// /}_${numproc};`./parse_results -i make.txt`" >> ${aggregate_file}
fi