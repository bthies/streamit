#!/bin/csh
# this script runs all of the generate tests

./do_generate_programs.pl scripts/fir.script
./do_generate_programs.pl scripts/sample.script
./do_generate_programs.pl scripts/target.script
./do_generate_programs.pl scripts/fm.script
./do_generate_programs.pl scripts/fb.script
./do_generate_programs.pl scripts/vocoder.script
./do_generate_programs.pl scripts/oversamp.script
./do_generate_programs.pl scripts/onebit.script
./do_generate_programs.pl scripts/bf.script


