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
	} else if (app.equals("matrix")) {
	    doMatrix16(str);
	} else if (app.equals("beam")) {
	    doBeam16(str);
	} else if (app.equals("beam2")) {
	    doBeam16_2(str);
	} else if (app.equals("beam3")) {
	    doBeam16_3(str);
	} else {
	    Utils.fail("no custom procedure for app \"" + app + "\" on " +
		       "granularity of " + num + ".");
	}
    }

    private static void doBeam16_2(SIRStream str) {
	RawFlattener rawFlattener;

	StreamItDot.printGraph(str, "unbalanced.dot");

	System.err.println("Working on Beamformer 2...");

	/*
	FieldProp.doPropagate(str);
	ConstantProp.propagateAndUnroll(str);

	rawFlattener = new RawFlattener(str);
	rawFlattener.dumpGraph("before-adjust.dot");
	System.err.println("\nBEFORE: " + rawFlattener.getNumTiles() + 
			   " tiles");
	*/

	//	WorkEstimate.getWorkEstimate(str).printWork();

	SIRPipeline pipe = (SIRPipeline)((SIRPipeline)str).get(0);
	FuseSplit.doFlatten(pipe);
	FuseAll.fuse(pipe);

	StreamItDot.printGraph(str, "balanced_0.dot");

	FuseSplit.doFlatten(pipe);
	FuseAll.fuse(pipe);

	// fuse magnitude and detector
	for (int i=0; i<2; i++) {
	    SIRFilter f1 = (SIRFilter) 
		((SIRPipeline) ((SIRSplitJoin) ((SIRPipeline)str).get(1) ).get(i) ).get(1);
	    SIRFilter f2 = (SIRFilter) 
		((SIRPipeline) ((SIRSplitJoin) ((SIRPipeline)str).get(1) ).get(i) ).get(3);
	    FusePipe.fuse(f1, f2);
	    SIRTwoStageFilter fused = (SIRTwoStageFilter)
		((SIRPipeline) ((SIRSplitJoin) ((SIRPipeline)str).get(1) ).get(i) ).get(1);
	    //fizz the combo
	    StatelessDuplicate.doit(fused, 4);
	}

	StreamItDot.printGraph(str, "balanced.dot");

	ConstantProp.propagateAndUnroll(str);
	//FieldProp.doPropagate(str);

	Namer.assignNames(str);
    }

    private static void doBeam16(SIRStream str) {
	RawFlattener rawFlattener;

	StreamItDot.printGraph(str, "unbalanced.dot");

	System.err.println("Working on Beamformer...");

	/*
	FieldProp.doPropagate(str);
	ConstantProp.propagateAndUnroll(str);

	rawFlattener = new RawFlattener(str);
	rawFlattener.dumpGraph("before-adjust.dot");
	System.err.println("\nBEFORE: " + rawFlattener.getNumTiles() + 
			   " tiles");
	*/

	WorkEstimate.getWorkEstimate(str).printWork();

	// fuse the beamfirfilters
	for (int i=0; i<2; i++) {
	    SIRFilter f1 = (SIRFilter) 
		((SIRPipeline) ((SIRSplitJoin) ((SIRPipeline)str).get(0) ).get(i) ).get(0);
	    SIRFilter f2 = (SIRFilter) 
		((SIRPipeline) ((SIRSplitJoin) ((SIRPipeline)str).get(0) ).get(i) ).get(1);
	    FusePipe.fuse(f1, f2);
	    // get the fused guy
	    SIRFilter fused = (SIRFilter) 
		((SIRPipeline) ((SIRSplitJoin) ((SIRPipeline)str).get(0) ).get(i) ).get(0);
	    // fizz him twice 
	    /*
	    StatelessDuplicate.doit(fused, 2);
	    */
	}

	// fuse downstream sj
	for (int i=0; i<2; i++) {
	    SIRFilter f1 = (SIRFilter) 
		((SIRPipeline) ((SIRSplitJoin) ((SIRPipeline)str).get(1) ).get(i) ).get(0);
	    SIRFilter f2 = (SIRFilter) 
		((SIRPipeline) ((SIRSplitJoin) ((SIRPipeline)str).get(1) ).get(i) ).get(3);
	    FusePipe.fuse(f1, f2);
	    StreamItDot.printGraph(str, (i)+".dot");
	    SIRTwoStageFilter fused = (SIRTwoStageFilter)
		((SIRPipeline) ((SIRSplitJoin) ((SIRPipeline)str).get(1) ).get(i) ).get(0);
	    System.err.println("trying to fiss: " + fused + " " + fused.getName());
	    System.err.println("the downstream fused filter has\n  pop=" + (fused.getPopInt())
			       + "\n  peek=" + (fused.getPeekInt())
			       + "\n  push=" + (fused.getPushInt()) 
			       + "\n  initPop=" + (fused.getInitPop()) 
			       + "\n  initPeek=" + (fused.getInitPeek()) 
			       + "\n  initPush=" + (fused.getInitPush()));
	    //fizz twice
	    StatelessDuplicate.doit(fused, 5);
	}

	StreamItDot.printGraph(str, "balanced.dot");

	ConstantProp.propagateAndUnroll(str);
	//FieldProp.doPropagate(str);

	Namer.assignNames(str);
	/*
	rawFlattener = new RawFlattener(str);
	rawFlattener.dumpGraph("after-adjust.dot");
	System.err.println("\nAFTER: " + rawFlattener.getNumTiles() + 
			   " tiles");
	*/
	//	WorkEstimate.getWorkEstimate(str).printWork();
    }

    private static void doBeam16_3(SIRStream str) {
	RawFlattener rawFlattener;

	StreamItDot.printGraph(str, "unbalanced.dot");

	System.err.println("Working on Beamformer...");

	/*
	FieldProp.doPropagate(str);
	ConstantProp.propagateAndUnroll(str);

	rawFlattener = new RawFlattener(str);
	rawFlattener.dumpGraph("before-adjust.dot");
	System.err.println("\nBEFORE: " + rawFlattener.getNumTiles() + 
			   " tiles");
	*/

	WorkEstimate.getWorkEstimate(str).printWork();

	// fuse the input-generate sj
	FuseSplit.fuse((SIRSplitJoin)((SIRPipeline)str).get(0));
	// fuse the pipeline resulting
	SIRFilter f1 = (SIRFilter) ((SIRPipeline)str).get(0);
	SIRFilter f2 = (SIRFilter) ((SIRPipeline)str).get(1);
	FusePipe.fuse(f1, f2);

	// fuse the first inputgenerate 

	// fuse the beamfirfilter
	/*
	  for (int i=0; i<2; i++) {
	  SIRFilter f1 = (SIRFilter) 
	  ((SIRPipeline) ((SIRSplitJoin) ((SIRPipeline)str).get(0) ).get(i) ).get(0);
	  SIRFilter f2 = (SIRFilter) 
	  ((SIRPipeline) ((SIRSplitJoin) ((SIRPipeline)str).get(0) ).get(i) ).get(1);
	  FusePipe.fuse(f1, f2);
	  // get the fused guy
	  SIRFilter fused = (SIRFilter) 
	  ((SIRPipeline) ((SIRSplitJoin) ((SIRPipeline)str).get(0) ).get(i) ).get(0);
	  // fizz him twice 
	  StatelessDuplicate.doit(fused, 2);
	}
	*/

	// fuse downstream sj
	for (int i=0; i<2; i++) {
	    f1 = (SIRFilter) 
		((SIRPipeline) ((SIRSplitJoin) ((SIRPipeline)str).get(2) ).get(i) ).get(0);
	    f2 = (SIRFilter) 
		((SIRPipeline) ((SIRSplitJoin) ((SIRPipeline)str).get(2) ).get(i) ).get(3);
	    FusePipe.fuse(f1, f2);
	    StreamItDot.printGraph(str, (i)+".dot");
	    SIRTwoStageFilter fused = (SIRTwoStageFilter)
		((SIRPipeline) ((SIRSplitJoin) ((SIRPipeline)str).get(2) ).get(i) ).get(0);
	    System.err.println("trying to fiss: " + fused + " " + fused.getName());
	    System.err.println("the downstream fused filter has\n  pop=" + (fused.getPopInt())
			       + "\n  peek=" + (fused.getPeekInt())
			       + "\n  push=" + (fused.getPushInt()) 
			       + "\n  initPop=" + (fused.getInitPop()) 
			       + "\n  initPeek=" + (fused.getInitPeek()) 
			       + "\n  initPush=" + (fused.getInitPush()));
	    //fizz twice
	    StatelessDuplicate.doit(fused, 5);
	}

	StreamItDot.printGraph(str, "balanced.dot");

	ConstantProp.propagateAndUnroll(str);
	//FieldProp.doPropagate(str);

	Namer.assignNames(str);
	/*
	rawFlattener = new RawFlattener(str);
	rawFlattener.dumpGraph("after-adjust.dot");
	System.err.println("\nAFTER: " + rawFlattener.getNumTiles() + 
			   " tiles");
	*/
	//	WorkEstimate.getWorkEstimate(str).printWork();
    }

    private static void doMatrix16(SIRStream str) {
	RawFlattener rawFlattener;

	StreamItDot.printGraph(str, "before.dot");

	System.err.println("Working on Matrix multiply...");
	rawFlattener = new RawFlattener(str);
	rawFlattener.dumpGraph("before-adjust.dot");
	System.err.println("\nBEFORE: " + rawFlattener.getNumTiles() + 
			   " tiles");

	//	FieldProp.doPropagate(str);
	//	ConstantProp.propagateAndUnroll(str);

	WorkEstimate.getWorkEstimate(str).printWork();

	FuseAll.fuse(str);
	FuseSplit.doFlatten(str);

	Namer.assignNames(str);
	rawFlattener = new RawFlattener(str);
	rawFlattener.dumpGraph("after-adjust.dot");
	System.err.println("\nAFTER: " + rawFlattener.getNumTiles() + 
			   " tiles");
	WorkEstimate.getWorkEstimate(str).printWork();
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

	StreamItDot.printGraph(str, "unfused.dot");
	//SIRScheduler.printGraph(str, "unfused.dot");

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

	//	FieldProp.doPropagate(str);

	//	FuseSplit.doFlatten(str);
	// get the inner splitjoin and fuse its pipes
	SIRSplitJoin split = (SIRSplitJoin)Namer.getStream("EqualizerSplitJoin_2_3_1");
	//FuseAll.fuse(split);

	//now fizz each side of the split-join by 3
	//StatelessDuplicate.doit((SIRFilter)split.get(0),3);
	//StatelessDuplicate.doit((SIRFilter)split.get(1),3);
	// constant-prop through new filters
	ConstantProp.propagateAndUnroll(split);
	//	FieldProp.doPropagate(split);

	// fuse the second two filters
	SIRPipeline pipe = (SIRPipeline)((SIRPipeline)str).get(1);
	/*
	System.err.println("Trying to fuse " + pipe.get(0) + " " + 
			   ((SIRStream)pipe.get(0)).getName() + " and " + pipe.get(1) +
			   " " + ((SIRStream)pipe.get(1)).getName());
	FusePipe.fuse((SIRFilter)pipe.get(0),
		      (SIRFilter)pipe.get(1));
	*/

	// fizzzzz the lpf 3 ways
	//StatelessDuplicate.doit((SIRFilter)pipe.get(0), 3);
	// constant-prop through new filters
	//ConstantProp.propagateAndUnroll(pipe);
	//FieldProp.doPropagate(str);

	// fuse the last three filters
	/*
	SIRFilter adder = (SIRFilter)Namer.getStream("FloatSubtract_2_3_2");
	SIRFilter printer = (SIRFilter)Namer.getStream("FloatPrinter_2_3_4");
	FusePipe.fuse(adder, printer);
	*/

	//	Namer.assignNames(str);
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
