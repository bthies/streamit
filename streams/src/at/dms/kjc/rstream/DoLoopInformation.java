package at.dms.kjc.rstream;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import java.util.ListIterator;
import at.dms.kjc.flatgraph.*;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;


public class DoLoopInformation 
{
    public JLocalVariable induction;
    public JStatement init;
    public JExpression cond;
    public JStatement incr;
    
    public DoLoopInformation() 
    {
	induction = null;
	init = null;
	cond = null;
	incr = null;
    }
}    
