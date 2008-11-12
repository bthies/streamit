#!/bin/bash

print_usage() {
    echo
    echo "Usage:"
    echo
    echo "   FarmWork [-u] [-s N] [-e N] [-o options] [-w work_dir] benchmark_file"
    echo
    echo "Options:"
    echo "   -u : Update StreamIt compiler"
    echo "   -s : Start Tilera size         (default = 1)"
    echo "   -e : End Tilera size           (default = 8)"
    echo "   -o : StreamIt compiler options (default = \"\")"
    echo "   -w : Work directory to use     (default = `pwd`/work)"
    echo
    echo "   benchmark_file : File containing list of all benchmarks to run"
    echo
}

# Parse arguments
update_compiler=0
start_size=1
end_size=8
compile_options=""
work_dir="`pwd`/work"
benchmark_file=""

while getopts ":us:e:o:w:" opt; do
    case $opt in
	u  ) update_compiler=1 ;;
	s  ) start_size=$OPTARG ;;
	e  ) end_size=$OPTARG ;;
	o  ) compile_options=$OPTARG ;;
	w  ) work_dir=$OPTARG ;;
	\? ) print_usage
	     exit 1 ;;
    esac
done

shift $(($OPTIND - 1))

if [ -z "$@" ]; then
    print_usage
    exit 1
fi

benchmark_file=$1

# Check environment variables
if [ -z "$TILERA_HOME" ]; then
    echo '$TILERA_HOME must be defined'
    exit 1
fi

if [ -z "$STREAMIT_HOME" ]; then
    echo '$STREAMIT_HOME must be defined'
    exit 1
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

# Check that benchmark file exists
if [ ! -f ${benchmark_file} ]; then
   echo "benchmarks file does not exist"
   exit 1
fi

# Update StreamIt compiler
if [ ${update_compiler} -eq 1 ]; then
    cwd=`pwd`

    cd ${STREAMIT_HOME}
    cvs update -d
    cd src
    make

    cd ${cwd}
fi

# Read benchmark file into benchmark array
current=0
for i in $(cat ${benchmark_file}); do
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
for ((low=${start_size};$low<=${end_size};low+=2)); do
    sizes[$current]=$low
    current=$current+1

    if [ $low -eq 1 ];then
	low=0
    fi
done

# Check if aggregate results file already exists.  If so, remove it
if [ -f "${work_dir}/aggregate_results" ]; then
    rm ${work_dir}/aggregate_results
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
	worker_dir=${work_dir}/${benchmark_name}_${compile_options// /}_${size}

	# Construct worker directory and copy in parse_results executable
	if [ -d "$worker_dir" ]; then
	    rm -rf ${worker_dir}
	fi

	mkdir -p ${worker_dir}
	cp parse_results ${worker_dir}

	# Worker-specific Condor options
	echo "Arguments = ${size} ${benchmark} ${work_dir}/aggregate_results ${compile_options}" >> condor_config
	echo "InitialDir = ${worker_dir}" >> condor_config
	echo "queue" >> condor_config
	echo >> condor_config
    done
done

# Submit jobs to Condor using constructed configuration file
condor_submit condor_config