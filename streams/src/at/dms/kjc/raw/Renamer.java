package at.dms.kjc.raw;

import streamit.scheduler.*;

import at.dms.util.IRPrinter;
import at.dms.util.SIRPrinter;
import at.dms.kjc.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.lir.*;
import java.util.*;

public class Renamer extends EmptyStreamVisitor 
{
    public static RenameAll renamer;
    
    static 
    {
	renamer = new RenameAll();
    }
    

    public static void renameAll(SIRStream str) 
    {
	SIRStream toplevel = str;
	while (toplevel.getParent()!=null) {
	    toplevel = toplevel.getParent();
	}
	// name the stream structure
	IterFactory.createIter(toplevel).accept(new Renamer());
    }
    
    /* visit a filter */
    public void visitFilter(SIRFilter self,
			    SIRFilterIter iter) {
	renamer.renameFilterContents(self);
    }
}
