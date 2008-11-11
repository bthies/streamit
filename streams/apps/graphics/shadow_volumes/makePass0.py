#!/usr/uns/bin/python

import os

os.system( '/u/jiawen/bin/strcm Pass0.filterlist' )
os.system( 'java streamit.frontend.ToJava --library --output Pass0.java Pass0.str' )
os.system( 'scripts/Pass0_adjustJava.py Pass0.java' )
os.system( 'javac Pass0.java' )
os.system( 'java -Xmx1800M Pass0 > Pass0.zBuffer.xy' )
os.system( 'scripts/Pass0_zBuffer_xy_to_zBuffer.py Pass0.zBuffer.xy 600x600' )


