package at.dms.kjc.rstream;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import java.util.ListIterator;
import at.dms.kjc.flatgraph.*;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;

/**
 * This class store various information for a do loop both
 * while it is being collected and after do loop identification.
 *
 *
 * @author Michael Gordon
 */
public class DoLoopInformation 
{
    /** The induction variable */
    public JLocalVariable induction;
    /** The initialization statement in do loop form */
    public JStatement init;
    /** The condition in do loop form */
    public JExpression cond;
    /** The increment in do loop form */
    public JStatement incr;
    
    public DoLoopInformation() 
    {
	induction = null;
	init = null;
	cond = null;
	incr = null;
    }
}    
