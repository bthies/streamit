package at.dms.kjc.spacedynamic;

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.FlatVisitor;
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


public class FileVisitor implements StreamGraphVisitor, FlatVisitor {
    //true if the graph contains a fileReader
    public boolean foundReader;
    //a hashset containing the flatnodes of the fileReaders
    public HashSet fileReaders;
    //true if the graph contains a fileWriter
    public boolean foundWriter;
    //a hashset containing the flatnodes of the fileWriters
    public HashSet fileWriters;
    //a hashset containing the flatnodes of all the file manipulators
    //(both readers and writers
    public HashSet fileNodes;

    private StreamGraph streamGraph;

    public void visitStaticStreamGraph(StaticStreamGraph ssg) 
    {
	ssg.getTopLevel().accept(this, new HashSet(), false);
    }
	
    public FileVisitor(StreamGraph streamGraph) 
    {
	this.streamGraph = streamGraph;
	foundReader = false;
	foundWriter = false;
	fileReaders = new HashSet();
	fileWriters = new HashSet();
	fileNodes = new HashSet();
	
	streamGraph.getTopLevel().accept(this, null, true);

	//add everything to the fileNodes hashset
	SpaceDynamicBackend.addAll(fileNodes, fileReaders);
	SpaceDynamicBackend.addAll(fileNodes, fileWriters);
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

    public boolean connectedToFR(RawTile tile) {
	Iterator frs = fileReaders.iterator();
	while (frs.hasNext()) {
	    if (SpaceDynamicBackend.rawChip.areNeighbors(tile, 
							 streamGraph.getLayout().getTile((FlatNode)frs.next()))) 
		return true;
	}
	return false;
    }
}
