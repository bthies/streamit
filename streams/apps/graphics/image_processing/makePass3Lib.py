#!/usr/uns/bin/python

import os

print( '=======================' )
print( 'strc --library -i1 Pass3_PoissonDOF.str > Pass3_output.xy' )
os.system( 'strc --library -i1 Pass3_PoissonDOF.str > Pass3_output.xy' )
print( '=======================' )
print( '~/bin/xyToPPM Pass3_output.xy 600x600' )
os.system( '~/bin/xyToPPM Pass3_output.xy 600x600' )
print( '=======================' )
print( 'xv Pass3_output.xy.ppm' )
os.system( 'xv Pass3_output.xy.ppm' )
