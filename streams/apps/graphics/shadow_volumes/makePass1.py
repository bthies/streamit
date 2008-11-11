#!/usr/uns/bin/python

import os

os.system( '/u/jiawen/bin/strcm Pass1.filterlist' )
os.system( 'java streamit.frontend.ToJava --library --output Pass1.java Pass1.str' )
os.system( 'scripts/Pass1_adjustJava.py Pass1.java' )
os.system( 'javac Pass1.java' )
os.system( 'java -Xmx1800M Pass1 > Pass1.sBuffer.xy' )
os.system( 'scripts/Pass1_sBuffer_xy_to_sBuffer.py Pass1.sBuffer.xy 600x600' )
