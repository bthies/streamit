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
	    ret.append("if (first" + buffer + " >= BUFSIZE) first" + 
		       buffer + " = 0;\n");
	    // ret.append("if (first" + buffer + " == last" + 
	    //       buffer + ") print_int(2000);\n");
	    
	}
	else { //receive
	    ret.append("buffer" + buffer + "[last" + buffer + "++] = static_receive");
	    if (fp)
		ret.append("_f");
	    ret.append("();\n");
	    ret.append("if (last" + buffer + " >= BUFSIZE) last" + buffer + " = 0;\n");
	}
	return ret.toString();
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
