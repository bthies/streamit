package at.dms.kjc.raw;

import at.dms.kjc.*;

public class JoinerScheduleNode 
{
    public static final int FIRE = 0;
    public static final int RECEIVE = 1;
    public static final int INITPATH = 2;
    public static final int DUPLICATE = 3;
    
    public JoinerScheduleNode next;
    public int type;
    public String buffer;
    public int initPathIndex = 0;

    public static final String DUPVAR = "__DUPVAR__";

    public JoinerScheduleNode() 
    {
    }
        
    //create an initPath call node...
    public JoinerScheduleNode(int index, String buf) {
	type = INITPATH;
	initPathIndex = index;
	buffer = buf;
    }
    
    public int getType() 
    {
	return type;
    }
    
    public String getBuffer() 
    {
	return buffer;
    }

    //print the c code for this node
    //fp is true if this is floating point code
    public String getC(CType ctype) {
	StringBuffer ret = new StringBuffer();
	StringBuffer arrayAccess = new StringBuffer();
	boolean fp = ctype.equals(CStdType.Float);
	String dims[] = null; 
	
	//true if the next node is a dup
	boolean nextDup = false;
	nextDup = (type == RECEIVE && next != null &&
		   next.type == DUPLICATE);
	
	if (ctype.isArrayType()) {
	    dims = Util.makeString(((CArrayType)ctype).getDims());
	    fp = ((CArrayType)ctype).getBaseType().equals(CStdType.Float);
	}
	
	if (ctype.isArrayType()) {
	    //code to loop thru all of the array elements
	    for (int i = 0; i < dims.length; i++) {
		ret.append("for (" + TileCode.ARRAY_INDEX + i + " = 0; " +
			   TileCode.ARRAY_INDEX + i + " < " + dims[i] + "; " + 
			   TileCode.ARRAY_INDEX + i + "++)\n");
	    }
	    //the code for the indexed access to the array
	    if (ctype.isArrayType()) {
		for (int i = 0; i < dims.length; i++) {
		    arrayAccess.append("[" + TileCode.ARRAY_INDEX + i + "]");
		}
	    }
	}
	ret.append("{\n");
   	if (type == DUPLICATE) {
	    ret.append(dupRecCode(arrayAccess));
	}
	else if (type == FIRE) {
	    ret.append("static_send_from_mem((void*)&__buffer" + buffer +
		       "[__first" + buffer + "++]");
	    ret.append(arrayAccess);
	    ret.append(");\n");
	    ret.append("}\n");
	    ret.append("__first" + buffer + " = __first" + buffer + " & __MINUSONE__;\n");
	    
	}
	else if (type == RECEIVE) { //receive
	    ret.append("static_receive_to_mem((void*)&");
	    if (nextDup) {
		ret.append(DUPVAR);
	    }
	    else {
		ret.append("__buffer" + buffer + "[__last" + buffer + "++]");
	    }
	    ret.append(arrayAccess);
	    //	    ret.append("= static_receive_to_mem");
	    //	    if (fp)
	    //		ret.append("_f");
	    ret.append(");\n");
	    if (nextDup) {
		ret.append(dupRecCode(arrayAccess));
	    }
	    else {
		ret.append("}\n");
		ret.append("__last" + buffer + " = __last" + buffer + " & __MINUSONE__;\n");
	    }
	}
	else if (type == INITPATH){
	    ret.append("__buffer" + buffer + "[__last" + buffer + "++]");
	    ret.append(arrayAccess);
	    ret.append("= initPath(" + 
		       initPathIndex + ");\n");
	    ret.append("}\n");
	    ret.append("__last" + buffer + " = __last" + buffer + " & __MINUSONE__;\n");
	}

	return ret.toString();
    }
    
    private StringBuffer dupRecCode(StringBuffer arrayAccess) {
	StringBuffer ret = new StringBuffer();
	ret.append("__buffer" + buffer + "[__last" + buffer + "++]");
	ret.append(arrayAccess);
	ret.append(" = " + DUPVAR);
	ret.append(arrayAccess);
	ret.append(";\n");
	ret.append("}\n");
	ret.append("__last" + buffer + " = __last" + buffer + " & __MINUSONE__;\n");
	return ret;
    }
    

    /**
     * Returns whether <other> has the same type and buffer name as
     * this.
     */
    public boolean equals(JoinerScheduleNode other) {
	if (type == DUPLICATE)
	    return false;
	else 
	    return type==other.type && buffer.equals(other.buffer) && initPathIndex == other.initPathIndex;
    }

    /**
     * Traverses the list defined from <node> and returns an array of
     * all elements reachable through the list.  result[0] is node,
     * and result[result.length-1].next = null.
     */
    public static JoinerScheduleNode[] toArray(JoinerScheduleNode node) {
	// figure that two traversals is cheaper than making a
	// linkedlist and going to an array

	// figure out how many elements in the list
	int count = 0;
	for (JoinerScheduleNode n=node; n!=null; n=n.next) {
	    count++;
	}

	// make output array
	JoinerScheduleNode[] result = new JoinerScheduleNode[count];
	for (int i=0; i<count; i++) {
	    result[i] = node;
	    node = node.next;
	}
	return result;
    }
    
    public void printMe() 
    {
	if (type == FIRE)
	    System.out.print("Fire: ");
	else if (type == RECEIVE) 
	    System.out.print("Receive: ");
	else if (type == INITPATH) {
	    System.out.println("InitPath: " + initPathIndex);
	    return;
	}
	System.out.println(buffer);
    }

    public static void printSchedule(JoinerScheduleNode first) 
    {
	do {
	    first.printMe();
	    first = first.next;
	}while(first != null);
    }
    
}
