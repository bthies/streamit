package at.dms.kjc.sir.lowering;

import java.util.*;
import at.dms.util.*;
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
	} else if (app.equals("fm")) {
	    // do custom transforms for FM radio
	    if (num==16) {
		doFM16_2(str);
	    }
	} else {
	    Utils.fail("no custom procedure for app \"" + app + "\" on " +
		       "granularity of " + num + ".");
	}
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

	System.out.println("Here's the new IR: ");
	SIRPrinter printer1 = new SIRPrinter();
	str.accept(printer1);
	printer1.close();
    }

    private static void doFM16_2(SIRStream str) {
	RawFlattener rawFlattener;

	System.err.println("Working on FM 16...");
	rawFlattener = new RawFlattener(str);
	rawFlattener.dumpGraph("before-adjust.dot");
	System.err.println("\nBEFORE: " + rawFlattener.getNumTiles() + 
			   " tiles");
	WorkEstimate.getWorkEstimate(str).printWork();

	SJFlatten.doFlatten(str);
	// get the inner splitjoin and fuse its pipes
	SIRSplitJoin split = (SIRSplitJoin)Namer.getStream("EqualizerSplitJoin_2_3_1");
	FuseAll.fuse(split);
	
	// now fizz each side of the split-join by 3
	StatelessDuplicate.doit((SIRFilter)((SIRPipeline)split.get(0)).get(0),
				3);
	StatelessDuplicate.doit((SIRFilter)((SIRPipeline)split.get(1)).get(0),
				3);
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
