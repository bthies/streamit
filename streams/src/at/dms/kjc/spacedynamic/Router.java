package at.dms.kjc.spacedynamic;

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.util.Utils;
import java.util.HashSet;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Vector;
import java.util.List;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Iterator;

public interface Router 
{
    public LinkedList<ComputeNode> getRoute(SpdStaticStreamGraph ssg, ComputeNode src, ComputeNode dst);

    
}
