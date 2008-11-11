lib_pass0:
	/u/jiawen/bin/strcm Pass0.filterlist
	java streamit.frontend.ToJava --library --output Pass0.java Pass0.str
	scripts/Pass0_adjustJava.py Pass0.java
	javac Pass0.java
	java -Xmx1800M Pass0 > Pass0.zBuffer.xy
	scripts/Pass0_zBuffer_xy_to_zBuffer.py Pass0.zBuffer.xy 600x600

lib_pass1:
	/u/jiawen/bin/strcm Pass1.filterlist
	java streamit.frontend.ToJava --library --output Pass1.java Pass1.str
	scripts/Pass1_adjustJava.py Pass1.java
	javac Pass1.java
	java -Xmx1800M Pass1 > Pass1.sBuffer.xy
	scripts/Pass1_sBuffer_xy_to_sBuffer.py Pass1.sBuffer.xy 600x600

raw4:
	~/bin/strcm; strc --spacedynamic --malloczeros -r 4 GPUModel.str

raw8:
	~/bin/strcm; strc --spacedynamic --malloczeros -r 8 GPUModel.str

clean:
	rm -f #*
	rm -f *.dot
	rm -f *.java
	rm -f *.class
	rm -f *.c
	rm -f *.out
	rm -f *.s
	rm -f *.h
	rm -f *.bc
	rm -f *.txt
	rm -f *.rlnk
	rm -f *.hic
	rm -f *.hwc
	rm -f *.o
	rm -f *.rexe
	rm -f kernel
	rm -f *.rbf*
	rm -f *.ppm
	rm -f *.arr
	rm -f *.xy
	rm -f Makefile.streamit
