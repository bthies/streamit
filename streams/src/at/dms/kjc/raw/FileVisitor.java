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


public class FileVisitor implements FlatVisitor {
    //true if the graph contains a fileReader
    public static boolean foundReader;
    //a hashset containing the flatnodes of the fileReaders
    public static HashSet fileReaders;
    //true if the graph contains a fileWriter
    public static boolean foundWriter;
    //a hashset containing the flatnodes of the fileWriters
    public static HashSet fileWriters;
    //a hashset containing the flatnodes of all the file manipulators
    //(both readers and writers
    public static HashSet fileNodes;

    public static void init(FlatNode top) {
	FileVisitor frv = new FileVisitor();
	top.accept(frv, new HashSet(), false);
	//add everything to the fileNodes hashset
	RawBackend.addAll(fileNodes, fileReaders);
	RawBackend.addAll(fileNodes, fileWriters);
    }
	
    public FileVisitor() 
    {
	foundReader = false;
	foundWriter = false;
	fileReaders = new HashSet();
	fileWriters = new HashSet();
	fileNodes = new HashSet();
    }
	
    public void visitNode (FlatNode node) 
    {
	if (node.contents instanceof SIRFileReader) {
	    fileReaders.add(node);
	    foundReader = true;
	}
	else if (node.contents instanceof SIRFileWriter) {
	    foundWriter = true;
	    fileWriters.add(node);
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
