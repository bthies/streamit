#!/bin/csh
# this script runs all of the operation tests

./do_operation_programs.pl scripts/fir.script
./do_operation_programs.pl scripts/sample.script
./do_operation_programs.pl scripts/target.script
./do_operation_programs.pl scripts/fm.script
./do_operation_programs.pl scripts/fb.script
./do_operation_programs.pl scripts/bf.script
./do_operation_programs.pl scripts/vocoder.script
./do_operation_programs.pl scripts/oversamp.script
./do_operation_programs.pl scripts/onebit.script



