/*
 * StreamType.java: record class for recording I/O types of streams
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: StreamType.java,v 1.1 2002-06-12 17:57:41 dmaze Exp $
 */

package streamit.frontend.tojava;

public class StreamType
{    
    public String fromType, toType;
    
    // Helper to return a Class object name corresponding to a type.
    public static String typeToClass(String s)
    {
        if (s.equals("int"))
            return "Integer.TYPE";
        else if (s.equals("float"))
            return "Float.TYPE";
        else if (s.equals("double"))
            return "Double.TYPE";
        else
            return s + ".class";
    }

    // Helpers to get the names of particular functions:
    public String pushFunction()
    {
        return annotatedFunction("output.push", toType);
    }
    
    public String popFunction()
    {
        return annotatedFunction("input.pop", fromType);
    }
    
    public String peekFunction()
    {
        return annotatedFunction("input.peek", fromType);
    }
    
    private static String annotatedFunction(String name, String type)
    {
        String suffix = "";
        // Check for known suffixes:
        if (type.equals("int"))
            suffix = "Int";
        else if (type.equals("float"))
            suffix = "Float";
        else if (type.equals("double"))
            suffix = "Double";
        return name + suffix;
    }   
}
