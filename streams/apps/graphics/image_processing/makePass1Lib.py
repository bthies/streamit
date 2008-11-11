#!/usr/uns/bin/python

import os

print( '=======================' )
print( 'strc --library -i1 Pass1_Downsampling.str > Pass1_output.xyrgb' )
os.system( 'strc --library -i1 Pass1_Downsampling.str > Pass1_output.xyrgb' )
print( '=======================' )
print( '~/bin/xyToPPM Pass1_output.xyrgb 150x150' )
os.system( '~/bin/xyToPPM Pass1_output.xyrgb 150x150' )
print( '=======================' )
print( 'scripts/Pass1_xyrgb_to_arrays.py Pass1_output.xyrgb 150x150 Pass2_input' )
os.system( 'scripts/Pass1_xyrgb_to_arrays.py Pass1_output.xyrgb 150x150 Pass2_input' )
