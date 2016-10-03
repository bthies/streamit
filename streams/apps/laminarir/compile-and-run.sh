#!/bin/bash

# LaminarIR benchmarks plus figure 1, a peeking filter, and a two-stage filter
declare -a dir=("autocor" "beamformer" "compcount" "dct" "des" "fft2" "fft2_256" "jpeg" "lattice" "matmul" "radixsort" "serpent" "figure1" "simple-peek" "simple-twostage")
declare -a str=("AutoCor.str" "BeamFormer.str" "ComparisonCounting.str" "DCT.str" "DES.str" "FFT2.str" "FFT2_256.str" "JPEGFilt.str" "Lattice.str" "MatrixMult.str" "RadixSort.str" "Serpent.str" "Figure1.str" "SimplePeek.str" "SimpleTwoStage.str")

arraylength=${#dir[@]}

# clean up non-releasable items
rm -f */smp1 */*.o */compiler-output */library-output pipe *~ */*~ */#* */*.class */*.gph
# clean up releasable items
rm -f */*.dot */*.c */*.cpp */*.java */Makefile */*.h */*.txt

for (( i=0; i<${arraylength}; i++ ));
do
    echo
    echo '----------------------------------------------------------------------'
    echo ${dir[$i]}
    cd ${dir[$i]}

    echo
    echo 'Compile and run' ${dir[$i]}':'
    strc -O2 -unroll 10000 ${str[$i]}
    ./smp1 | head -100000 > compiler-output
    rm -f ../pipe; mkfifo ../pipe ; strc -library ${str[$i]} > ../pipe & head -100000 ../pipe > library-output; \
    ps -ef | grep java | grep $USER | grep -v grep | awk '{print $2}' | xargs kill

    echo
    echo 'Check output of' ${dir[$i]}' ('`wc -l library-output | awk '{print $1}'` 'lines):'
    if [[ "${dir[$i]}" = "des" || "${dir[$i]}" = "serpent" ]]; then
	diff compiler-output library-output && echo "No differences found"
    else
	numdiff -r 1e-5 -a 1e-5 compiler-output library-output
    fi

    echo
    echo 'Check for FIFOs in' ${dir[$i]}':'
    grep '\[' core0.c | grep -i _buf || echo "Found nothing"

    cd ..
done

