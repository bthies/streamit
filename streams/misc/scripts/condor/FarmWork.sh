#!/bin/bash

# Check for correct usage
if [ $# -ne 4 ]; then
    echo "usage:"
    echo -e "    ./FarmWork lower_tile_size upper_tile_size compiler_options benchmark_file"
    exit 1
fi

# Check environment variables
if [ -z "$TILERA_HOME" ]; then
    echo '$TILERA_HOME must be defined'
    exit 1
fi

if [ -z "$STREAMIT_HOME" ]; then
    echo '$STREAMIT_HOME must be defined'
    exit 1
fi

if [ -z "$WORK_DIR" ]; then
    export WORK_DIR=`pwd`/work
fi

# Check that necessary files for farming exist
if [ ! -f parse_results ]; then
    echo "parse_results executable is missing"
    exit 1
fi

if [ ! -f "condor_config.header" ]; then
    echo "Condor configuration header file is missing"
    exit 1
fi

# Check that benchmark files exists
if [ ! -f "$4" ]; then
   echo "benchmarks file does not exist"
   exit 1
fi

# Read benchmark file into benchmark array
current=0
for i in $(cat $4); do
    i=$(eval echo $i)
    if [ -f "$i" ]; then  
	benchmarks[$current]=$i
	current=$current+1 
    else
	echo "File $i does not exist, skipping it."
    fi
done

# Set up sizes array to hold Tilera sizes we want to simulate
current=0
for ((low=$1;$low<=$2;low+=2)); do
    sizes[$current]=$low
    current=$current+1

    if [ $low -eq 1 ];then
	low=0
    fi
done

# Check if aggregate results file already exists.  If so, remove it
if [ -f "${WORK_DIR}/aggregate_results" ]; then
    rm ${WORK_DIR}/aggregate_results
fi

# Construct Condor configuration file
if [ -f "condor_config" ]; then
    rm condor_config
fi

cp condor_config.header condor_config

echo 'Executable = Worker.sh' >> condor_config    # Global Condor options
echo 'Log = log.txt' >> condor_config
echo 'Error = error.txt' >> condor_config
echo 'Output = output.txt' >> condor_config
echo 'Rank = (machine != "underdog.csail.mit.edu")' >> condor_config
echo >> condor_config

for benchmark in "${benchmarks[@]}"; do
    benchmark_file=${benchmark##/*/}
    benchmark_name=${benchmark_file%.str}

    for size in "${sizes[@]}"; do
	worker_dir=${WORK_DIR}/${benchmark_name}_${3// /}_${size}

	# Construct worker directory and copy in parse_results executable
	if [ -d "$worker_dir" ]; then
	    rm -rf ${worker_dir}
	fi

	mkdir -p ${worker_dir}
	cp parse_results ${worker_dir}

	# Worker-specific Condor options
	echo "Arguments = ${size} '${3}' ${benchmark} ${WORK_DIR}/aggregate_results" >> condor_config
	echo "InitialDir = ${worker_dir}" >> condor_config
	echo "queue" >> condor_config
	echo >> condor_config
    done
done

# Submit jobs to Condor using constructed configuration file
#condor_submit condor_config