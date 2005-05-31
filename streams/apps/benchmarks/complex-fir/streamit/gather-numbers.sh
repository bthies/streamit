#/usr/bin/bash

# Used for gathering numbers for Ian's thesis, comparing performance
# on this application to reptile.

# NOTE!  This script did not work for me as-is, I just
# cut-and-pasted the whole thing (starting rm -rf results)
# into my terminal.  Something with environment vars...

export STREAMIT_HOME=/u/thies/research/streams/streams
. /u/thies/research/streams/streams/include/dot-bashrc

rm -rf results
mkdir results

# N=2
perl -pi -e 's/N\=16/N\=2/g' ComplexFIR.str

strc -r1 -O2 -altcodegen -N 64 ComplexFIR.str
make -f Makefile.streamit run
mv results.out results-N2-single-tile.out

strc -r2 -c2 -O2 -altcodegen -N 64 ComplexFIR.str	
make -f Makefile.streamit run
mv results.out results-N2-4-tile-data-parallel.out

strc -r2 -c4 -O2 -altcodegen -N 64 ComplexFIR.str
make -f Makefile.streamit run
mv results.out results-N2-8-tile-data-parallel.out

strc -r4 -O2 -altcodegen -N 64 ComplexFIR.str
make -f Makefile.streamit run
mv results.out results-N2-16-tile-data-parallel.out

strc -r4 -O2 -altcodegen -N 64 -fission 16 ComplexFIR.str
make -f Makefile.streamit run
mv results.out results-N2-16-tile-pipeline-parallel.out

# N=4
perl -pi -e 's/N\=2/N\=4/g' ComplexFIR.str

strc -r1 -O2 -altcodegen -N 64 ComplexFIR.str
make -f Makefile.streamit run
mv results.out results-N4-single-tile.out

strc -r2 -c2 -O2 -altcodegen -N 64 ComplexFIR.str	
make -f Makefile.streamit run
mv results.out results-N4-4-tile-data-parallel.out

strc -r2 -c4 -O2 -altcodegen -N 64 ComplexFIR.str
make -f Makefile.streamit run
mv results.out results-N4-8-tile-data-parallel.out

strc -r4 -O2 -altcodegen -N 64 ComplexFIR.str
make -f Makefile.streamit run
mv results.out results-N4-16-tile-data-parallel.out

strc -r4 -O2 -altcodegen -N 64 -fission 16 ComplexFIR.str
make -f Makefile.streamit run
mv results.out results-N4-16-tile-pipeline-parallel.out

# N=8
perl -pi -e 's/N\=4/N\=8/g' ComplexFIR.str

strc -r1 -O2 -altcodegen -N 64 ComplexFIR.str
make -f Makefile.streamit run
mv results.out results-N8-single-tile.out

strc -r2 -c2 -O2 -altcodegen -N 64 ComplexFIR.str	
make -f Makefile.streamit run
mv results.out results-N8-4-tile-data-parallel.out

strc -r2 -c4 -O2 -altcodegen -N 64 ComplexFIR.str
make -f Makefile.streamit run
mv results.out results-N8-8-tile-data-parallel.out

strc -r4 -O2 -altcodegen -N 64 ComplexFIR.str
make -f Makefile.streamit run
mv results.out results-N8-16-tile-data-parallel.out

strc -r4 -O2 -altcodegen -N 64 -fission 16 ComplexFIR.str
make -f Makefile.streamit run
mv results.out results-N8-16-tile-pipeline-parallel.out

# N=16
perl -pi -e 's/N\=8/N\=16/g' ComplexFIR.str

strc -r1 -O2 -altcodegen -N 64 ComplexFIR.str
make -f Makefile.streamit run
mv results.out results-N16-single-tile.out

strc -r2 -c2 -O2 -altcodegen -N 64 ComplexFIR.str	
make -f Makefile.streamit run
mv results.out results-N16-4-tile-data-parallel.out

strc -r2 -c4 -O2 -altcodegen -N 64 ComplexFIR.str
make -f Makefile.streamit run
mv results.out results-N16-8-tile-data-parallel.out

strc -r4 -O2 -altcodegen -N 64 ComplexFIR.str
make -f Makefile.streamit run
mv results.out results-N16-16-tile-data-parallel.out

strc -r4 -O2 -altcodegen -N 64 -fission 16 ComplexFIR.str
make -f Makefile.streamit run
mv results.out results-N16-16-tile-pipeline-parallel.out
