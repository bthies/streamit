#!/bin/csh
# this script runs all of the timing tests

./do_timing_programs.pl scripts/fir.script
./do_timing_programs.pl scripts/sample.script
./do_timing_programs.pl scripts/target.script
./do_timing_programs.pl scripts/fm.script
./do_timing_programs.pl scripts/fb.script
./do_timing_programs.pl scripts/bf.script
./do_timing_programs.pl scripts/vocoder.script
./do_timing_programs.pl scripts/oversamp.script
./do_timing_programs.pl scripts/onebit.script



