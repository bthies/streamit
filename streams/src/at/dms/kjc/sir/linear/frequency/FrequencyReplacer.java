package at.dms.kjc.sir.linear.frequency;

import java.util.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.linear.*;
import at.dms.kjc.iterator.*;
import at.dms.compiler.*;


/**
 * This class is the interface and base class for the frequency replacement functionality of
 * the linear analysis framework. At one point there were multiple replacers
 * that did slightly different things (niave vs. optimized, fftw vs. non fftw).
 * However, now there is one very slick frequency replacer (LEETFrequencyReplacer)
 * that is always used.<br>
 *
 * $Id: FrequencyReplacer.java,v 1.10 2003-10-24 22:04:03 thies Exp $
 **/
public abstract class FrequencyReplacer extends LinearReplacer implements Constants{
    /**
     * Start the process of replacement on str using the Linearity information in lfa.
     **/
    public static void doReplace(LinearAnalyzer lfa, SIRStream str) {
	LinearPrinter.println("Beginning frequency replacement)...");

	FrequencyReplacer replacer = new LEETFrequencyReplacer(lfa);
		
	// pump the replacer through the stream graph.
	IterFactory.createFactory().createIter(str).accept(replacer);
    }

}
