# ~/bin/strcm; strc --spacedynamic --library GPUModel.str

#~/bin/strcm; strc --library GPUModel.str
#~/bin/strcm; java streamit.frontend.ToJava --library --output GPUModel.java GPUModel.str; ~/bin/adjustJava GPUModel.java; javac GPUModel.java
#~/bin/strcm; java streamit.frontend.ToJava --library --output GPUModel.java GPUModel.str

lib:
	java streamit.frontend.ToJava --library --output GPUModel.java GPUModel.str Input.str PixelShaderB.str PixelShader.str Rasterizer.str RasterOps.str TriangleSetup.str Types.str VertexShader.str
	../scripts/adjustJava.py GPUModel.java
	javac GPUModel.java
	java -Xmx1800M GPUModel > colorBuffer.xy
	../scripts/xyToPPM.py colorBuffer.xy 600x600

raw8:
	strc --spacedynamic -N3 --malloczeros -r8 GPUModel.str Input.str PixelShaderB.str PixelShader.str Rasterizer.str RasterOps.str TriangleSetup.str Types.str VertexShader.str

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
	rm -rf *.lnk
	rm -f Makefile.streamit
