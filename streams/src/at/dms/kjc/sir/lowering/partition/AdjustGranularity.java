package at.dms.kjc.sir.lowering.partition;

import java.util.*;
import at.dms.util.*;
import at.dms.kjc.*;
import at.dms.kjc.iterator.*;
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
	    doFM16_2(str);
	} else if (app.equals("fft")) {
	    // do custom transforms for fft
	    if (num!=1) {
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
	} else if (app.equals("beam4")) {
	    doBeam16_4(str);
	} else if (app.equals("beam4x4")) {
	    doBeam16_4_4(str);
	} else if (app.equals("bb1")) {
	    doBigBeam1(str, 2);
	} else if (app.equals("bb2")) {
	    doBigBeam2(str);
	} else if (app.equals("bb3")) {
	    doBigBeam1(str, 3);
	} else if (app.equals("bb4")) {
	    doBigBeam1OnlyBottomSplit(str);
	} else if (app.equals("bb620")) {
            // june 20, for retreat talk
	    doBigBeam620(str);
	} else if (app.equals("crc32")) {
	    doCRC32(str);
	} else if (app.equals("vectadd")) {
	    doVectAdd(str);
	} else if (app.equals("fft3")) {
	    doFFT3(str);
	} else {
	    Utils.fail("no custom procedure for app \"" + app + "\" on " +
		       "granularity of " + num + ".");
	}
    }

    private static void doBigBeam1(SIRStream _str, int numSplits) {
	SIRPipeline str = (SIRPipeline)_str;	

	StreamItDot.printGraph(str, "unbalanced.dot");
	System.err.println("Working on the big beamform...");

	FuseAll.fuse(str);

	// fuse hierarchical splitjoins
	for (int k=0; k<numSplits; k++) {
	    SIRSplitJoin sj = (SIRSplitJoin)str.get(k);
	    for (int i=0; i<sj.size(); i++) {
		SIRSplitJoin toFuse = (SIRSplitJoin)((SIRPipeline)(sj.get(i))).get(0);
	        SIRStream result = FuseSplit.fuse(toFuse);
		if (result==toFuse) {
		    System.err.println("Rejected for fusion: " + toFuse);
		}
		FusePipe.fuse((SIRPipeline)sj.get(i));
	    }
	}
	
	/*
	FuseSplit.fuse((SIRSplitJoin)str.get(1));
	StreamItDot.printGraph(str, "1.dot");
	FuseSplit.fuse((SIRSplitJoin)str.get(3));
	FusePipe.fuse((SIRFilter)str.get(3),
		      (SIRFilter)str.get(4));
	*/

	//ConstantProp.propagateAndUnroll(str);
	//FieldProp.doPropagate(str);

	StreamItDot.printGraph(str, "balanced.dot");
    }

    private static void doBigBeam620(SIRStream _str) {
	SIRPipeline str = (SIRPipeline)_str;	

	StreamItDot.printGraph(str, "unbalanced.dot");
	System.err.println("Working on the big beamform...");

	FieldProp.doPropagate(str);

	FuseAll.fuse(str);

	// fuse hierarchical splitjoins
	SIRSplitJoin sj = (SIRSplitJoin)str.get(1);
	for (int i=0; i<sj.size(); i++) {
	    SIRSplitJoin toFuse = (SIRSplitJoin)((SIRPipeline)(sj.get(i))).get(0);
	    SIRStream result = FuseSplit.fuse(toFuse);
	    if (result==toFuse) {
		System.err.println("Rejected for fusion: " + toFuse);
	    }
	    FusePipe.fuse((SIRPipeline)sj.get(i));
	}
	
	/*
	FuseSplit.fuse((SIRSplitJoin)str.get(1));
	StreamItDot.printGraph(str, "1.dot");
	FuseSplit.fuse((SIRSplitJoin)str.get(3));
	FusePipe.fuse((SIRFilter)str.get(3),
		      (SIRFilter)str.get(4));
	*/

	//ConstantProp.propagateAndUnroll(str);
	//FieldProp.doPropagate(str);

	StreamItDot.printGraph(str, "balanced.dot");
    }

    private static void doBigBeam1OnlyBottomSplit(SIRStream _str) {
	SIRPipeline str = (SIRPipeline)_str;	

	StreamItDot.printGraph(str, "unbalanced.dot");
	System.err.println("Working on the big beamform...");

	FieldProp.doPropagate(str);

	// do top
	SIRSplitJoin sj = (SIRSplitJoin)str.get(0);
	for (int i=0; i<sj.size(); i++) {
	    SIRPipeline pipe = (SIRPipeline)sj.get(i);
	    FusePipe.fuse(pipe);
	}

	// do bottom
	sj = (SIRSplitJoin)str.get(1);
	for (int i=0; i<sj.size(); i++) {
	    SIRSplitJoin toFuse = (SIRSplitJoin)((SIRPipeline)(sj.get(i))).get(0);
	    FusePipe.fuse((SIRPipeline)toFuse.get(0));
	    FusePipe.fuse((SIRPipeline)toFuse.get(1));
	    FuseSplit.fuse(toFuse);
	    FusePipe.fuse((SIRPipeline)sj.get(i));
	}
	
	StreamItDot.printGraph(str, "balanced.dot");
    }

    private static void doBigBeam2(SIRStream _str) {
	SIRPipeline str = (SIRPipeline)_str;	

	StreamItDot.printGraph(str, "unbalanced.dot");
	System.err.println("Working on big beamform #2...");

	FuseAll.fuse(str);

	StreamItDot.printGraph(str, "1.dot");

	// fuse hierarchical splitjoins
	/*
	for (int k=1; k<2; k++) {
	    SIRSplitJoin sj = (SIRSplitJoin)str.get(k);
	    for (int i=0; i<sj.size(); i++) {
		SIRSplitJoin toFuse = (SIRSplitJoin)((SIRPipeline)(sj.get(i))).get(0);
	        SIRStream result = FuseSplit.fuse(toFuse);
		if (result==toFuse) {
		    System.err.println("Rejected for fusion: " + toFuse);
		}
		// (2) for second one, fuse resulting pipeline
		if (k==1) { 
		    FusePipe.fuse((SIRPipeline)sj.get(i));
		}
	    }
	}
	
	// (2) fuse third sj and pipe
	FuseSplit.fuse((SIRSplitJoin)str.get(2));
	FusePipe.fuse((SIRFilter)str.get(2),
		      (SIRFilter)str.get(3));
	*/

	SIRSplitJoin split1 = (SIRSplitJoin)str.get(1);
	SIRSplitJoin split2 = (SIRSplitJoin)str.get(3);
	SIRSplitJoin split3 = (SIRSplitJoin)str.get(5);

	FuseSplit.fuse(split1);
	FuseSplit.fuse(split2);
	FuseSplit.fuse(split3);
	/*
	FusePipe.fuse((SIRFilter)str.get(3),
		      (SIRFilter)str.get(4));
	*/

	//ConstantProp.propagateAndUnroll(str);
	//FieldProp.doPropagate(str);

	StreamItDot.printGraph(str, "balanced.dot");
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
	FuseAll.fuse(pipe);

	StreamItDot.printGraph(str, "balanced_0.dot");

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
		((SIRPipeline) ((SIRSplitJoin) ((SIRPipeline)str).get(0) ).get(i) ).get(1);
	    SIRFilter f2 = (SIRFilter) 
		((SIRPipeline) ((SIRSplitJoin) ((SIRPipeline)str).get(0) ).get(i) ).get(2);
	    FusePipe.fuse(f1, f2);
	    /*
	    // get the fused guy
	    SIRFilter fused = (SIRFilter) 
		((SIRPipeline) ((SIRSplitJoin) ((SIRPipeline)str).get(0) ).get(i) ).get(0);
	    // fizz him twice 
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

	/*
	rawFlattener = new RawFlattener(str);
	rawFlattener.dumpGraph("after-adjust.dot");
	System.err.println("\nAFTER: " + rawFlattener.getNumTiles() + 
			   " tiles");
	*/
	//	WorkEstimate.getWorkEstimate(str).printWork();
    }

    private static void doBeam16_4(SIRStream str) {
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

	FieldProp.doPropagate(str);
	WorkEstimate.getWorkEstimate(str).printWork();

	// fuse the top splitjoin
	for (int i=0; i<2; i++) {
	    SIRFilter f1 = (SIRFilter) 
		((SIRPipeline) ((SIRSplitJoin) ((SIRPipeline)str).get(0) ).get(i) ).get(0);
	    SIRFilter f2 = (SIRFilter) 
		((SIRPipeline) ((SIRSplitJoin) ((SIRPipeline)str).get(0) ).get(i) ).get(2);
	    FusePipe.fuse(f1, f2);
	    Lifter.eliminatePipe((SIRPipeline)((SIRSplitJoin) ((SIRPipeline)str).get(0) ).get(i));
	}

	// fuse the splitjoin
	FuseSplit.fuse((SIRSplitJoin)((SIRPipeline)str).get(0));

	SIRFilter f1 = (SIRFilter)((SIRPipeline)str).get(0);
	SIRFilter f2 = (SIRFilter)((SIRPipeline)str).get(1);
	FusePipe.fuse(f1, f2);

	// split it
	StatelessDuplicate.doit((SIRFilter)((SIRPipeline)str).get(0), 3);

	StreamItDot.printGraph(str, "balanced0.dot");

	// fuse downstream sj
	for (int i=0; i<2; i++) {
	    f1 = (SIRFilter) 
		((SIRPipeline) ((SIRSplitJoin) ((SIRPipeline)str).get(1) ).get(i) ).get(0);
	    f2 = (SIRFilter) 
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

	/*
	rawFlattener = new RawFlattener(str);
	rawFlattener.dumpGraph("after-adjust.dot");
	System.err.println("\nAFTER: " + rawFlattener.getNumTiles() + 
			   " tiles");
	*/
	//	WorkEstimate.getWorkEstimate(str).printWork();
    }

    private static void doBeam16_4_4(SIRStream str) {
	// fuse the top splitjoin
	for (int i=0; i<4; i++) {
	    SIRPipeline pipe = (SIRPipeline) ((SIRSplitJoin) ((SIRPipeline)str).get(0) ).get(i);

	    SIRFilter f1 = (SIRFilter) pipe.get(0);
	    SIRFilter f2 = (SIRFilter) pipe.get(1);

	    FusePipe.fuse(f1, f2);
	}

	// fuse downstream sj
	SIRSplitJoin split = (SIRSplitJoin) ((SIRPipeline)str).get(1);
	for (int i=0; i<4; i++) {
	    SIRPipeline pipe = (SIRPipeline)split.get(i);

	    FusePipe.fuse(pipe);
	}
	/*
	FuseSplit.fuse(split);
	*/

	StreamItDot.printGraph(str, "after.dot");
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

	// fuse the beamfirfilter
	for (int i=0; i<2; i++) {
	    f1 = (SIRFilter) 
		((SIRPipeline) ((SIRSplitJoin) ((SIRPipeline)str).get(1) ).get(i) ).get(0);
	     f2 = (SIRFilter) 
		((SIRPipeline) ((SIRSplitJoin) ((SIRPipeline)str).get(1) ).get(i) ).get(1);
	    FusePipe.fuse(f1, f2);
	    // get the fused guy
	    SIRFilter fused = (SIRFilter) 
		((SIRPipeline) ((SIRSplitJoin) ((SIRPipeline)str).get(1) ).get(i) ).get(0);
	  // fizz him twice 
	  StatelessDuplicate.doit(fused, 2);
	}

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

	/*
	rawFlattener = new RawFlattener(str);
	rawFlattener.dumpGraph("after-adjust.dot");
	System.err.println("\nAFTER: " + rawFlattener.getNumTiles() + 
			   " tiles");
	*/
	//	WorkEstimate.getWorkEstimate(str).printWork();
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

	StreamItDot.printGraph(str, "unfused.dot");

	int ch;
	for (ch=0;ch<2;ch++)
        {
		System.out.println ("doing channel " + ch);
		System.out.println (str);
		System.out.println (((SIRPipeline)str).get (1));
		System.out.println ((((SIRPipeline)(((SIRPipeline)str).get (1))).get (0)));
		System.out.println (((SIRSplitJoin)((((SIRPipeline)(((SIRPipeline)str).get (1))).get (0)))).get (ch));
		SIRPipeline channel = (SIRPipeline)(((SIRSplitJoin)((((SIRPipeline)(((SIRPipeline)str).get (1))).get (0)))).get (ch));

		FuseAll.fuse ((SIRSplitJoin)channel.get (3));
		StreamItDot.printGraph(str, "fused1.dot");

		FusePipe.fuse ((SIRFilter)channel.get (3), (SIRFilter)channel.get (4));
		FusePipe.fuse ((SIRFilter)channel.get (3), (SIRFilter)channel.get (4));

		StreamItDot.printGraph (str, "fused3.dot");

		FuseAll.fuse ((SIRSplitJoin)(((SIRPipeline)channel.get (4))).get(3));

		StreamItDot.printGraph (str, "fused4.dot");
	}

	/*

	FuseAll.fuse(str);
	FuseSplit.doFlatten(str);

	StreamItDot.printGraph(str, "fused1.dot");

	FuseAll.fuse(str);
	FuseSplit.doFlatten(str);

	StreamItDot.printGraph(str, "fused2.dot");

	FuseAll.fuse(str);
	FuseSplit.doFlatten(str);

	StreamItDot.printGraph(str, "fused3.dot");

	FuseAll.fuse(str);
	FuseSplit.doFlatten(str);

	StreamItDot.printGraph(str, "fused4.dot");

	*/

        ConstantProp.propagateAndUnroll(str);

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

        ConstantProp.propagateAndUnroll(str);

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

	rawFlattener = new RawFlattener(str);
	rawFlattener.dumpGraph("after-adjust.dot");
	System.err.println("\nAFTER: " + rawFlattener.getNumTiles() + 
			   " tiles");
	WorkEstimate.getWorkEstimate(str).printWork();
    }

    private static void doCRC32(SIRStream str) {
	RawFlattener rawFlattener;

	System.err.println("Working on crc32...");
	rawFlattener = new RawFlattener(str);
	rawFlattener.dumpGraph("before-adjust.dot");
	System.err.println("\nBEFORE: " + rawFlattener.getNumTiles() + 
			   " tiles");

	{
	  SIRFeedbackLoop f = (SIRFeedbackLoop)(((SIRPipeline)str).get (1));
	  SIRPipeline body = (SIRPipeline)f.getBody ();
	  FusePipe.fuse (body);
	}

	FieldProp.doPropagate(str);
	ConstantProp.propagateAndUnroll(str);

	StreamItDot.printGraph (str, "fused1.dot");

	WorkEstimate.getWorkEstimate(str).printWork();

	rawFlattener = new RawFlattener(str);
	rawFlattener.dumpGraph("after-adjust.dot");
	System.err.println("\nAFTER: " + rawFlattener.getNumTiles() + 
			   " tiles");
	WorkEstimate.getWorkEstimate(str).printWork();
    }

    private static void doMatrix16(SIRStream str) {
	RawFlattener rawFlattener;

	System.err.println("Working on MatrixMult...");
	rawFlattener = new RawFlattener(str);
	rawFlattener.dumpGraph("before-adjust.dot");
	System.err.println("\nBEFORE: " + rawFlattener.getNumTiles() + 
			   " tiles");
        {
            SIRStream s = ((SIRPipeline)str).get (1);
	    s = ((SIRPipeline)s).get (0);
	    SIRSplitJoin sj = (SIRSplitJoin)s;

            FuseAll.fuse(sj.get (0));

            ConstantProp.propagateAndUnroll(s);
        }
	FieldProp.doPropagate(str);
	ConstantProp.propagateAndUnroll(str);

	StreamItDot.printGraph (str, "fused1.dot");

	WorkEstimate.getWorkEstimate(str).printWork();

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

	StreamItDot.printGraph(str, "unfused.dot");

	WorkEstimate.getWorkEstimate(str).printWork();

	StreamItDot.printGraph(str, "unfused.dot");
	//SIRScheduler.printGraph(str, "unfused.dot");

	// fuse shortonesource, rpeinputfilter
	{
	    SIRFilter f1 = (SIRFilter)((SIRPipeline)str).get (1);
	    SIRFilter f2 = (SIRFilter)((SIRPipeline)str).get (2);
	    //	SIRFilter rpeDecode = Namer.getStream("RPEDecodeFilter_3");
	    FusePipe.fuse(f1, f2);
	}


	StreamItDot.printGraph (str, "fused1.dot");

	/*
	// fose post-processing filter, short sink
	SIRFilter postProc = (SIRFilter)Namer.getStream("PostProcessingFilter_8");
	SIRFilter sink = (SIRFilter)Namer.getStream("ShortPrinterSink_9");
	FusePipe.fuse(postProc, sink);
	*/
	{
	    SIRStream s = ((SIRPipeline)str).get (4);

            FuseAll.fuse(s);

            ConstantProp.propagateAndUnroll(s);
	}
	
	{
	    SIRFilter f1 = (SIRFilter)((SIRPipeline)str).get (4);
	    SIRFilter f2 = (SIRFilter)((SIRPipeline)str).get (5);

            FusePipe.fuse(f1, f2);
	}
	
	{
	    SIRFilter f1 = (SIRFilter)((SIRPipeline)str).get (4);
	    SIRFilter f2 = (SIRFilter)((SIRPipeline)str).get (5);

            FusePipe.fuse(f1, f2);

	}

        ConstantProp.propagateAndUnroll(str);

        {
	    SIRFeedbackLoop loop = (SIRFeedbackLoop)((SIRPipeline)str).get(2);
	    SIRPipeline pipe = (SIRPipeline)loop.getLoop ();
	    SIRSplitJoin sj = (SIRSplitJoin)pipe.get (0);
	    FuseAll.fuse (sj);
        }

        ConstantProp.propagateAndUnroll(str);
	

	StreamItDot.printGraph (str, "fused2.dot");

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

	FieldProp.doPropagate(str);

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

    private static void doFM16_2(SIRStream _str) {
	SIRPipeline str = (SIRPipeline)_str;
	RawFlattener rawFlattener;

	System.err.println("Working on FM 16...");
	/*
	rawFlattener = new RawFlattener(str);
	rawFlattener.dumpGraph("before-adjust.dot");
	System.err.println("\nBEFORE: " + rawFlattener.getNumTiles() + 
			   " tiles");
	*/
	FieldProp.doPropagate(str);
	WorkEstimate.getWorkEstimate(str).printWork();
	StreamItDot.printGraph(str, "before.dot");

	// fizzzzz the low-pass filter
	SIRFilter lowPass = (SIRFilter)Namer.getStream("LowPassFilter_2_1");
	StatelessDuplicate.doit(lowPass, 2);
	// constant-prop through new filters
	ConstantProp.propagateAndUnroll(lowPass.getParent());
	FieldProp.doPropagate((lowPass.getParent()));

	/* fuse the splitjoin in each stream */
	SIRSplitJoin split1 = (SIRSplitJoin)Namer.getStream("BandPassSplitJoin_2_3_1_2_1");
	SIRSplitJoin split2 = (SIRSplitJoin)Namer.getStream("BandPassSplitJoin_2_3_1_3_1");
	SIRPipeline pipe1 = (SIRPipeline)split1.getParent();
	SIRPipeline pipe2 = (SIRPipeline)split2.getParent();

	/*
	// fuse the splits
	FuseSplit.fuse(split1);
	FuseSplit.fuse(split2);
	
	// fuse the results
	FusePipe.fuse(pipe1);
	FusePipe.fuse(pipe2);
	*/
	
	// fuse the final result
	//	SIRSplitJoin split = (SIRSplitJoin)Namer.getStream("EqualizerSplitJoin_2_3_1");

	/*
	Lifter.eliminatePipe((SIRPipeline)split.get(0));
	Lifter.eliminatePipe((SIRPipeline)split.get(1));
	*/
	/*
	FuseSplit.fuse(split);
	*/

	/*
	// then fizz it three times
	StatelessDuplicate.doit((SIRFilter)pipe1.get(0), 3);
	StatelessDuplicate.doit((SIRFilter)pipe2.get(0), 3);
	*/

	/* this fizzes the lpf's 2 ways each *
	   StatelessDuplicate.doit((SIRFilter)Namer.getStream("LowPassFilter_2_3_1_2_1_2"), 2);
	   StatelessDuplicate.doit((SIRFilter)Namer.getStream("LowPassFilter_2_3_1_2_1_3"), 2);
	   StatelessDuplicate.doit((SIRFilter)Namer.getStream("LowPassFilter_2_3_1_3_1_2"), 2);
	   StatelessDuplicate.doit((SIRFilter)Namer.getStream("LowPassFilter_2_3_1_3_1_3"), 2);
	   /**/
	
	// fuse the last three filters
	//	FusePipe.fuse((SIRPipeline)((SIRPipeline)str.get(1)).get(2));

	// split the result
	//	StatelessDuplicate.doit((SIRFilter)((SIRPipeline)str.get(1)).get(2), 4);
	//	StatelessDuplicate.doit((SIRFilter)((SIRPipeline)((SIRPipeline)str.get(1)).get(2)).get(0), 6);

	SIRFilter adder = (SIRFilter)Namer.getStream("FloatSubtract_2_3_2");
	SIRFilter printer = (SIRFilter)Namer.getStream("FloatPrinter_2_3_4");
	FusePipe.fuse(adder, printer);

	ConstantProp.propagateAndUnroll(str);
	FieldProp.doPropagate(str);	
	/*
	ConstantProp.propagateAndUnroll(pipe2);
	FieldProp.doPropagate(pipe2);
	*/

	StreamItDot.printGraph(str, "after.dot");

	/*
	rawFlattener = new RawFlattener(str);
	rawFlattener.dumpGraph("after-adjust.dot");
	System.err.println("\nAFTER: " + rawFlattener.getNumTiles() + 
			   " tiles");
	WorkEstimate.getWorkEstimate(str).printWork();
	*/

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

	FuseAll.fuse(str);
	ConstantProp.propagateAndUnroll(str);

	rawFlattener = new RawFlattener(str);
	rawFlattener.dumpGraph("after-adjust.dot");
	System.err.println("\nAFTER: " + rawFlattener.getNumTiles() + 
			   " tiles");
	WorkEstimate.getWorkEstimate(str).printWork();
	StreamItDot.printGraph(str, "after.dot");
    }

    //for testing SyncRemovalSJPair and place-holder for other stuff 
    private static void doVectAdd(SIRStream str) { 
	SIRPipeline topPipe = (SIRPipeline)str; 
	System.err.println("Working on VectAdd...");
	
	Utils.assert(!(topPipe.get(0) instanceof SIRSplitJoin), 
	             "Custom transformations for \"VectAdd with a splitjoin source\" not supported now!"); 
	
	SIRFilter src = (SIRFilter) topPipe.get(0); 
	SIRFilter add = (SIRFilter) topPipe.get(1); 
	SIRFilter sink = (SIRFilter) topPipe.get(2); 
		
	//Do a 4-way stateless duplicate 
        //(since src and snk have a field ('idx') whose value persists across invocations, they 
        //couldn't fissioned in the current code). So test SyncRemovalSJPair using other dummy kernels 
        //between src, add and add, snk.   
	//StatelessDuplicate.doit(src, 4); 
	StatelessDuplicate.doit(add, 4); 
	//StatelessDuplicate.doit(sink, 4);

	StreamItDot.printGraph(topPipe, "after-stateless.dot"); 

	//Do a sync-removal transformation 
	//first do the first and second (src and add) 
	//SyncRemovalSJPair.diffuseSJPair((SIRSplitJoin)topPipe.get(0), (SIRSplitJoin)topPipe.get(1)); 
	//StreamItDot.printGraph(topPipe, "after-syncremov1.dot"); 
	//then the resultant diffused SJ and the third (sink).  
	//SyncRemovalSJPair.diffuseSJPair((SIRSplitJoin)topPipe.get(0), (SIRSplitJoin)topPipe.get(1)); 
	//StreamItDot.printGraph(topPipe, "after-syncremov2.dot"); 
    } 	

    //for testing SyncRemovalSJPair but needs hierarchical SJ reorderer before that could be done. place-holder for other stuff too.  
    private static void doFFT3(SIRStream str) { 
	SIRPipeline topPipe = (SIRPipeline)str; 
	System.err.println("Working on FFT3...");
	
	//SIRFilter src = (SIRFilter) topPipe.get(0); 
	//SIRFilter add = (SIRFilter) topPipe.get(1); 
	//SIRFilter sink = (SIRFilter) topPipe.get(2); 
		
	//Do a sync-removal transformation 
	//first do the first and second (src and add) 
	//SyncRemovalSJPair.diffuseSJPair((SIRSplitJoin)topPipe.get(0), (SIRSplitJoin)topPipe.get(1)); 
	//StreamItDot.printGraph(topPipe, "after-syncremov1.dot"); 
	//then the resultant diffused SJ and the third (sink).  
	//SyncRemovalSJPair.diffuseSJPair((SIRSplitJoin)topPipe.get(0), (SIRSplitJoin)topPipe.get(1)); 
	//StreamItDot.printGraph(topPipe, "after-syncremov2.dot"); 
    } 	
}
