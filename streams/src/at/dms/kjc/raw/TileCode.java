package at.dms.kjc.raw;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.util.Utils;
import java.util.List;
import java.util.ListIterator;
import java.util.LinkedList;
import java.util.HashSet;

/**
 * This class dumps the tile code for each filter into a file based 
 * on the tile number assigned 
 */
public class TileCode extends at.dms.util.Utils implements FlatVisitor {
    public static void generateCode(FlatNode topLevel) 
    {
	topLevel.accept(new TileCode(), new HashSet(), true);
    }
    
    public TileCode() 
    {
	
    }
    
    public void visitNode(FlatNode node) 
    {
	if (node.contents instanceof SIRFilter) {
	    FlatIRToC.generateCode(node);
	}
    }
}

