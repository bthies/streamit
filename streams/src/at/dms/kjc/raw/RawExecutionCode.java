package at.dms.kjc.raw;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.iterator.*;
import at.dms.util.Utils;
import java.util.List;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.HashMap;
import java.io.*;
import at.dms.compiler.*;
import at.dms.kjc.sir.lowering.*;
import java.util.Hashtable;

public class RawExecutionCode extends at.dms.util.Utils 
    implements FlatVisitor 
{

    public static void doit(FlatNode top) 
    {
	top.accept((new RawExecutionCode()), null, true);
    }
    
    public void visitNode(FlatNode node) 
    {
	if (node.isFilter()){
	    
	}
    } 
}

