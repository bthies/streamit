package at.dms.kjc.sir.lowering;

import java.util.*;
import at.dms.util.*;
import at.dms.kjc.*;
import at.dms.kjc.raw.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.fission.*;
import at.dms.kjc.sir.lowering.fusion.*;

public class AdjustGranularity {


    /**
     * Tries to adjusts <str> into <num> pieces of equal work.
     */
    public static void doit(SIRStream str, int num) {
	// test for custom application
	String app = System.getProperty("app", "unknown");
	// for unknown app, do nothing
	if (app.equals("unknown")) {
	    return;
	}
	if (app.equals("fm")) {
	    // do custom transforms for FM radio
	    if (num==-1) {
		Namer.assignNames(str);
	    }
	    doFM16_2(str);
	} else if (app.equals("fft")) {
	    // do custom transforms for fft
	    if (num==16) {
		doFFT16(str);
	    } else {
		doFFT1(str);
	    }
	} else if (app.equals("gsm")) {
	    if (num==16) {
		doGSM16(str);
	    }
	} else if (app.equals("pca")) {
	    doPCA(str);
	} else if (app.equals("mp3")) {
	    if (num==64) {
		doMP3_64(str);
	    } else {
		doMP3_16(str);
	    }
	} else {
	    Utils.fail("no custom procedure for app \"" + app + "\" on " +
		       "granularity of " + num + ".");
	}
    }

    private static void doMP3_16(SIRStream str) {
	RawFlattener rawFlattener;

	System.err.println("Working on MP3...");
	rawFlattener = new RawFlattener(str);
	rawFlattener.dumpGraph("before-adjust.dot");
	System.err.println("\nBEFORE: " + rawFlattener.getNumTiles() + 
			   " tiles");

	//	FieldProp.doPropagate(str);
	//	ConstantProp.propagateAndUnroll(str);

	WorkEstimate.getWorkEstimate(str).printWork();

	FuseAll.fuse(str);
	FuseSplit.doFlatten(str);
	FuseAll.fuse(str);
	FuseSplit.doFlatten(str);
	FuseAll.fuse(str);
	FuseSplit.doFlatten(str);
	FuseAll.fuse(str);
	FuseSplit.doFlatten(str);

	Namer.assignNames(str);
	rawFlattener = new RawFlattener(str);
	rawFlattener.dumpGraph("after-adjust.dot");
	System.err.println("\nAFTER: " + rawFlattener.getNumTiles() + 
			   " tiles");
	WorkEstimate.getWorkEstimate(str).printWork();
    }

    private static void doMP3_64(SIRStream str) {
	RawFlattener rawFlattener;

	System.err.println("Working on MP3...");
	rawFlattener = new RawFlattener(str);
	rawFlattener.dumpGraph("before-adjust.dot");
	System.err.println("\nBEFORE: " + rawFlattener.getNumTiles() + 
			   " tiles");

	//	FieldProp.doPropagate(str);
	//	ConstantProp.propagateAndUnroll(str);

	WorkEstimate.getWorkEstimate(str).printWork();

	FuseAll.fuse(str);
	FuseSplit.doFlatten(str);
	FuseAll.fuse(str);
	FuseSplit.doFlatten(str);

	Namer.assignNames(str);
	rawFlattener = new RawFlattener(str);
	rawFlattener.dumpGraph("after-adjust.dot");
	System.err.println("\nAFTER: " + rawFlattener.getNumTiles() + 
			   " tiles");
	WorkEstimate.getWorkEstimate(str).printWork();
    }

    private static void doPCA(SIRStream str) {
	RawFlattener rawFlattener;

	System.err.println("Working on PCA...");
	rawFlattener = new RawFlattener(str);
	rawFlattener.dumpGraph("before-adjust.dot");
	System.err.println("\nBEFORE: " + rawFlattener.getNumTiles() + 
			   " tiles");

	//	FieldProp.doPropagate(str);
	//	ConstantProp.propagateAndUnroll(str);

	WorkEstimate.getWorkEstimate(str).printWork();

	Namer.assignNames(str);
	rawFlattener = new RawFlattener(str);
	rawFlattener.dumpGraph("after-adjust.dot");
	System.err.println("\nAFTER: " + rawFlattener.getNumTiles() + 
			   " tiles");
	WorkEstimate.getWorkEstimate(str).printWork();
    }

    private static void doGSM16(SIRStream str) {
	RawFlattener rawFlattener;

	System.err.println("Working on GSM...");
	rawFlattener = new RawFlattener(str);
	rawFlattener.dumpGraph("before-adjust.dot");
	System.err.println("\nBEFORE: " + rawFlattener.getNumTiles() + 
			   " tiles");

	//	FieldProp.doPropagate(str);
	//	ConstantProp.propagateAndUnroll(str);

	WorkEstimate.getWorkEstimate(str).printWork();

	// fuse shortonesource, rpeinputfilter
	SIRFilter source = (SIRFilter)Namer.getStream("ShortOneSource_1");
	SIRFilter rpeInput = (SIRFilter)Namer.getStream("RPEInputFilter_2");
	//	SIRFilter rpeDecode = Namer.getStream("RPEDecodeFilter_3");
	FusePipe.fuse(source, rpeInput);

	// fose post-processing filter, short sink
	SIRFilter postProc = (SIRFilter)Namer.getStream("PostProcessingFilter_8");
	SIRFilter sink = (SIRFilter)Namer.getStream("ShortPrinterSink_9");
	FusePipe.fuse(postProc, sink);

	Namer.assignNames(str);
	rawFlattener = new RawFlattener(str);
	rawFlattener.dumpGraph("after-adjust.dot");
	System.err.println("\nAFTER: " + rawFlattener.getNumTiles() + 
			   " tiles");
	WorkEstimate.getWorkEstimate(str).printWork();
    }

    private static void doFM16(SIRStream str) {
	RawFlattener rawFlattener;

	System.err.println("Working on FM 16...");
	rawFlattener = new RawFlattener(str);
	rawFlattener.dumpGraph("before-adjust.dot");
	System.err.println("\nBEFORE: " + rawFlattener.getNumTiles() + 
			   " tiles");
	WorkEstimate.getWorkEstimate(str).printWork();

	// fizzzzz the low-pass filter
	SIRFilter lowPass = (SIRFilter)Namer.getStream("LowPassFilter_2_1");
	StatelessDuplicate.doit(lowPass, 4);
	// constant-prop through new filters
	ConstantProp.propagateAndUnroll(lowPass.getParent());
	FieldProp.doPropagate((lowPass.getParent()));

	// fuse the last three filters
	SIRFilter adder = (SIRFilter)Namer.getStream("FloatSubtract_2_3_2");
	SIRFilter printer = (SIRFilter)Namer.getStream("FloatPrinter_2_3_4");
	FusePipe.fuse(adder, printer);

	Namer.assignNames(str);
	rawFlattener = new RawFlattener(str);
	rawFlattener.dumpGraph("after-adjust.dot");
	System.err.println("\nAFTER: " + rawFlattener.getNumTiles() + 
			   " tiles");
	WorkEstimate.getWorkEstimate(str).printWork();

	/*
	System.out.println("Here's the new IR: ");
	SIRPrinter printer1 = new SIRPrinter();
	str.accept(printer1);
	printer1.close();
	*/
    }

    private static void doFFT1(SIRStream str) {
	RawFlattener rawFlattener;

	StreamItDot.printGraph(str, "before.dot");
	System.err.println("Working on FFT...");
	rawFlattener = new RawFlattener(str);
	rawFlattener.dumpGraph("before-adjust.dot");
	System.err.println("\nBEFORE: " + rawFlattener.getNumTiles() + 
			   " tiles");
	WorkEstimate.getWorkEstimate(str).printWork();

	//FuseSplit.fuse((SIRSplitJoin)((SIRPipeline)((SIRPipeline)str).get(1)).get(2));
	//	FuseSplit.fuse((SIRSplitJoin)((SIRPipeline)((SIRPipeline)str).get(1)).get(1));

	FuseSplit.doFlatten(str);
	FuseAll.fuse(str);
	FuseSplit.doFlatten(str);
	FuseAll.fuse(str);
	ConstantProp.propagateAndUnroll(str);

	Namer.assignNames(str);
	rawFlattener = new RawFlattener(str);
	rawFlattener.dumpGraph("after-adjust.dot");
	System.err.println("\nAFTER: " + rawFlattener.getNumTiles() + 
			   " tiles");
	WorkEstimate.getWorkEstimate(str).printWork();
	StreamItDot.printGraph(str, "after.dot");
    }

    private static void doFFT16(SIRStream str) {
	RawFlattener rawFlattener;

	StreamItDot.printGraph(str, "before.dot");
	System.err.println("Working on FFT...");
	rawFlattener = new RawFlattener(str);
	rawFlattener.dumpGraph("before-adjust.dot");
	System.err.println("\nBEFORE: " + rawFlattener.getNumTiles() + 
			   " tiles");
	WorkEstimate.getWorkEstimate(str).printWork();

	// fuse the splits
	FuseSplit.doFlatten(str);

	StreamItDot.printGraph (str, "bill-fft1.dot");
	FusePipe.fuse((SIRPipeline)
		      ((SIRSplitJoin)
		       ((SIRPipeline)
			((SIRPipeline)str).get(1)).get(0)).get(0));
	StreamItDot.printGraph (str, "bill-fft2.dot");
	FusePipe.fuse((SIRPipeline)
		      ((SIRSplitJoin)
		       ((SIRPipeline)
			((SIRPipeline)str).get(1)).get(0)).get(1));

	StreamItDot.printGraph (str, "bill-fft3.dot");
	// now take the leftovers from the splitjoin fusion and fuse them, too
	SIRPipeline pipe = (SIRPipeline)((SIRPipeline)str).get(1);
	for (int i=0; i<10; i+=2) {
	    FusePipe.fuse((SIRFilter)pipe.get(i+1), (SIRFilter)pipe.get(i+3));
	    FusePipe.fuse((SIRFilter)pipe.get(i+2), (SIRFilter)pipe.get(i+3));
	}

	StreamItDot.printGraph (str, "bill-fft4.dot");

        FusePipe.fuse( (SIRFilter)(((SIRPipeline)((SIRPipeline)str).get(1)).get(7)),
                       (SIRFilter)(((SIRPipeline)((SIRPipeline)str).get(1)).get(8)) );
        FusePipe.fuse( (SIRFilter)(((SIRPipeline)((SIRPipeline)str).get(1)).get(4)),
                       (SIRFilter)(((SIRPipeline)((SIRPipeline)str).get(1)).get(5)) );

	StreamItDot.printGraph (str, "bill-fft5.dot");
	StatelessDuplicate.doit ((SIRFilter)((SIRSplitJoin)((SIRPipeline)((SIRPipeline)str).get (1)).get(0)).get(0),2);
	StreamItDot.printGraph (str, "bill-fft6.dot");
	StatelessDuplicate.doit ((SIRFilter)((SIRSplitJoin)((SIRPipeline)((SIRPipeline)str).get (1)).get(0)).get(1),2);

	// constant-prop through new filters
	ConstantProp.propagateAndUnroll(str);
	FieldProp.doPropagate(str);

	Namer.assignNames(str);
	rawFlattener = new RawFlattener(str);
	rawFlattener.dumpGraph("after-adjust.dot");
	System.err.println("\nAFTER: " + rawFlattener.getNumTiles() + 
			   " tiles");
	WorkEstimate.getWorkEstimate(str).printWork();
	StreamItDot.printGraph(str, "after.dot");
    }

    /*
	RawFlattener rawFlattener;

	StreamItDot.printGraph(str, "before.dot");
	System.err.println("Working on FFT...");
	rawFlattener = new RawFlattener(str);
	rawFlattener.dumpGraph("before-adjust.dot");
	System.err.println("\nBEFORE: " + rawFlattener.getNumTiles() + 
			   " tiles");
	WorkEstimate.getWorkEstimate(str).printWork();

	// fuse the splits
	FuseSplit.doFlatten(str);

	FusePipe.fuse((SIRPipeline)
		      ((SIRSplitJoin)
		       ((SIRPipeline)
			((SIRPipeline)str).get(1)).get(0)).get(0));
	FusePipe.fuse((SIRPipeline)
		      ((SIRSplitJoin)
		       ((SIRPipeline)
			((SIRPipeline)str).get(1)).get(0)).get(1));

	// now take the leftovers from the splitjoin fusion and fuse them, too
	SIRPipeline pipe = (SIRPipeline)((SIRPipeline)str).get(1);
	for (int i=0; i<10; i+=2) {
	    FusePipe.fuse((SIRFilter)pipe.get(i+1), (SIRFilter)pipe.get(i+3));
	    FusePipe.fuse((SIRFilter)pipe.get(i+2), (SIRFilter)pipe.get(i+3));
	}

	Namer.assignNames(str);
	rawFlattener = new RawFlattener(str);
	rawFlattener.dumpGraph("after-adjust.dot");
	System.err.println("\nAFTER: " + rawFlattener.getNumTiles() + 
			   " tiles");
	WorkEstimate.getWorkEstimate(str).printWork();
	StreamItDot.printGraph(str, "after.dot");
    }
    */

    private static void doFM16_2(SIRStream str) {
	RawFlattener rawFlattener;

	System.err.println("Working on FM 16...");
	rawFlattener = new RawFlattener(str);
	rawFlattener.dumpGraph("before-adjust.dot");
	System.err.println("\nBEFORE: " + rawFlattener.getNumTiles() + 
			   " tiles");
	WorkEstimate.getWorkEstimate(str).printWork();

	StreamItDot.printGraph(str, "1.dot");

	SJFlatten.doFlatten(str);
	// get the inner splitjoin and fuse its pipes
	SIRSplitJoin split = (SIRSplitJoin)Namer.getStream("EqualizerSplitJoin_2_3_1");
	FuseAll.fuse(split);

	// now fizz each side of the split-join by 3
	StatelessDuplicate.doit((SIRFilter)split.get(0),3);
	StatelessDuplicate.doit((SIRFilter)split.get(1),3);
	// constant-prop through new filters
	ConstantProp.propagateAndUnroll(split);
	FieldProp.doPropagate(split);

	// fuse the second two filters
	SIRPipeline pipe = (SIRPipeline)((SIRPipeline)str).get(1);
	FusePipe.fuse((SIRFilter)pipe.get(0),
		      (SIRFilter)pipe.get(1));

	// fizzzzz the fused combo 3 ways
	StatelessDuplicate.doit((SIRFilter)pipe.get(0), 4);
	// constant-prop through new filters
	ConstantProp.propagateAndUnroll(pipe);
	FieldProp.doPropagate(pipe);

	// fuse the last three filters
	SIRFilter adder = (SIRFilter)Namer.getStream("FloatSubtract_2_3_2");
	SIRFilter printer = (SIRFilter)Namer.getStream("FloatPrinter_2_3_4");
	FusePipe.fuse(adder, printer);

	Namer.assignNames(str);
	rawFlattener = new RawFlattener(str);
	rawFlattener.dumpGraph("after-adjust.dot");
	System.err.println("\nAFTER: " + rawFlattener.getNumTiles() + 
			   " tiles");
	WorkEstimate.getWorkEstimate(str).printWork();

	System.out.println("Here's the new IR: ");
	SIRPrinter printer1 = new SIRPrinter();
	str.accept(printer1);
	printer1.close();
    }

}
