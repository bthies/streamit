#!/usr/uns/bin/python

import os

os.system( '/u/jiawen/bin/strcm FILTERLIST' )
os.system( 'java streamit.frontend.ToJava --library --output GPUModel.java GPUModel.str' )
os.system( 'scripts/adjustJava.py GPUModel.java' )
os.system( 'javac GPUModel.java' )
os.system( 'java -Xmx1800M GPUModel > output.colorBuffer' )
os.system( '~/bin/xyToPPM output.colorBuffer 600x600' )
