#!/usr/uns/bin/python

import os

exec_string = '../../scripts/strcm.py GPUModel.filterlist'
print( exec_string )
os.system( exec_string )

exec_string = 'java streamit.frontend.ToJava --library --output GPUModel.java GPUModel.str'
print( exec_string )
os.system( exec_string )

exec_string = '../../scripts/adjustJava.py GPUModel.java'
print( exec_string )
os.system( exec_string )

exec_string = 'javac GPUModel.java'
print( exec_string )
os.system( exec_string )

exec_string = 'java -Xmx1800M GPUModel > colorBuffer.xy'
print( exec_string )
os.system( exec_string )

exec_string = '../../scripts/xyToPPM.py colorBuffer.xy 600x600'
print( exec_string )
os.system( exec_string )
