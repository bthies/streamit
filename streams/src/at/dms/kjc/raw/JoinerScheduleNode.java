package at.dms.kjc.raw;

public class JoinerScheduleNode 
{
    public static final int FIRE = 0;
    public static final int RECEIVE = 1;
       
    public JoinerScheduleNode next;
    public int type;
    public String buffer;
    
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
    public String getC(boolean fp) {
	StringBuffer ret = new StringBuffer();
	
	if (type == FIRE) {
	    ret.append("static_send(buffer" + buffer +
		       "[first" + buffer + "++]);\n");
	    ret.append("first" + buffer + " = first" + buffer + " & MINUSONE;\n");
	    //	    ret.append("if (first" + buffer + " >= BUFSIZE) first" + 
	    //	       buffer + " = 0;\n");
	    // ret.append("if (first" + buffer + " == last" + 
	    //       buffer + ") print_int(2000);\n");
	    
	}
	else { //receive
	    ret.append("buffer" + buffer + "[last" + buffer + "++] = static_receive");
	    if (fp)
		ret.append("_f");
	    ret.append("();\n");
	    ret.append("last" + buffer + " = last" + buffer + " & MINUSONE;\n");
	    //ret.append("if (last" + buffer + " >= BUFSIZE) last" + buffer + " = 0;\n");
	}
	return ret.toString();
    }
    
    /**
     * Returns whether <other> has the same type and buffer name as
     * this.
     */
    public boolean equals(JoinerScheduleNode other) {
	return type==other.type && buffer.equals(other.buffer);
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
	else 
	    System.out.print("Receive: ");
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
