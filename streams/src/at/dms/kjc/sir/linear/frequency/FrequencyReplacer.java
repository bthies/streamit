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
 * that did slightly different things. However, now there is one very slick frequency replacer
 * that is always used.
 **/
public abstract class FrequencyReplacer extends LinearReplacer implements Constants{
    /**
     * Start the process of replacement on str using the Linearity information in lfa.
     **/
    public static void doReplace(LinearAnalyzer lfa, SIRStream str) {
	LinearPrinter.println("Beginning leet frequency replacement)...");

	FrequencyReplacer replacer = new LEETFrequencyReplacer(lfa);
		
	// pump the replacer through the stream graph.
	IterFactory.createIter(str).accept(replacer);
    }

}
