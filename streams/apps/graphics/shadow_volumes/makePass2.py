#!/usr/uns/bin/python

import os

os.system( '/u/jiawen/bin/strcm Pass2.filterlist' )
os.system( 'java streamit.frontend.ToJava --library --output Pass2.java Pass2.str' )
os.system( 'scripts/Pass2_adjustJava.py Pass2.java' )
os.system( 'javac Pass2.java' )
os.system( 'java -Xmx1800M Pass2 > Pass2.colorBuffer.xy' )
os.system( '~/bin/xyToPPM Pass2.colorBuffer.xy 600x600' )
