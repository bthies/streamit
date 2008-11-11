#!/usr/uns/bin/python

import os

print( '=======================' )
print( '/u/jiawen/bin/strcm Pass0.filterlist' )
os.system( '/u/jiawen/bin/strcm Pass0.filterlist' )
print( '=======================' )
print( 'java streamit.frontend.ToJava --library --output Pass0.java Pass0.str' )
os.system( 'java streamit.frontend.ToJava --library --output Pass0.java Pass0.str' )
print( '=======================' )
print( 'scripts/Pass0_adjustJava.py Pass0.java' )
os.system( 'scripts/Pass0_adjustJava.py Pass0.java' )
print( '=======================' )
print( 'javac Pass0.java' )
os.system( 'javac Pass0.java' )
print( '=======================' )
print( 'java -Xmx1800M Pass0 > Pass0_output.xyrgbz' )
os.system( 'java -Xmx1800M Pass0 > Pass0_output.xyrgbz' )
print( '=======================' )
print( 'scripts/Pass0_xyrgbz_to_PPM_arr_eyeZBuffer.py Pass0_output.xyrgbz 600x600 Pass0_output' )
os.system( 'scripts/Pass0_xyrgbz_to_PPM_arr_eyeZBuffer.py Pass0_output.xyrgbz 600x600 Pass0_output' )
