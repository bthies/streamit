package at.dms.kjc.raw;

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.util.Utils;
import java.io.*;
import java.util.List;
import java.util.*;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;


public class FileReaderVisitor implements FlatVisitor {
    public static boolean foundReader;
    public static HashSet fileReaders;
	
    public static void init(FlatNode top) {
	FileReaderVisitor frv = new FileReaderVisitor();
	top.accept(frv, new HashSet(), false);
    }
	
    public FileReaderVisitor() 
    {
	foundReader = false;
	fileReaders = new HashSet();
    }
	
    public void visitNode (FlatNode node) 
    {
	if (node.contents instanceof SIRFileReader) {
	    fileReaders.add(node);
	    foundReader = true;
	}
    }

    public static boolean connectedToFR(Coordinate tile) {
	Iterator frs = fileReaders.iterator();
	while (frs.hasNext()) {
	    if (Layout.areNeighbors(tile, Layout.getTile((FlatNode)frs.next()))) 
		return true;
	}
	return false;
    }
}
