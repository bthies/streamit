#!/usr/uns/bin/python

import os

print( '=======================' )
print( 'strc --library -i1 Pass2_GaussianBlur.str > Pass2_output.xyrgb' )
os.system( 'strc --library -i1 Pass2_GaussianBlur.str > Pass2_output.xyrgb' )
print( '=======================' )
print( '~/bin/xyToPPM Pass2_output.xyrgb 150x150' )
os.system( '~/bin/xyToPPM Pass2_output.xyrgb 150x150' )
print( '=======================' )
print( 'scripts/Pass2_xyrgb_to_arrays.py Pass2_output.xyrgb 150x150 Pass3_input_low' )
os.system( 'scripts/Pass2_xyrgb_to_arrays.py Pass2_output.xyrgb 150x150 Pass3_input_low' )
