/*
 * StreamType.java: record class for recording I/O types of streams
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: StreamType.java,v 1.3 2002-07-15 18:58:17 dmaze Exp $
 */

package streamit.frontend.tojava;

import streamit.frontend.nodes.Type;
import streamit.frontend.nodes.TypeArray;
import streamit.frontend.nodes.TypePrimitive;
import streamit.frontend.nodes.TypeStruct;

public class StreamType
{    
    public Type fromType, toType;
    
    // Helper to return a Class object name corresponding to a type.
    public static String typeToClass(Type t)
    {
        if (t instanceof TypePrimitive)
        {
            switch (((TypePrimitive)t).getType())
            {
            case TypePrimitive.TYPE_INT:
                return "Integer.TYPE";
            case TypePrimitive.TYPE_FLOAT:
                return "Float.TYPE";
            case TypePrimitive.TYPE_DOUBLE:
                return "Double.TYPE";
            }
        }
        else if (t instanceof TypeStruct)
            return ((TypeStruct)t).getName() + ".class";
        // Errp.
        return null;
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
    
    private static String annotatedFunction(String name, Type type)
    {
        String prefix = "", suffix = "";
        // Check for known suffixes:
        if (type instanceof TypePrimitive)
        {
            switch (((TypePrimitive)type).getType())
            {
            case TypePrimitive.TYPE_INT:
                suffix = "Int";
                break;
            case TypePrimitive.TYPE_FLOAT:
                suffix = "Float";
                break;
            case TypePrimitive.TYPE_DOUBLE:
                suffix = "Double";
                break;
            }
        }
        if (type instanceof TypeStruct && name.startsWith("input"))
            prefix = "(" + ((TypeStruct)type).getName() + ")";
        return prefix + name + suffix;
    }   
}
