package at.dms.kjc;

import java.util.*;

public class SerializationVector 
{
    private static Vector vec;
    
    static
    {
	vec = new Vector();
    }
    
    public static Integer addObject(Object obj)
    {
	vec.add(obj);
	return new Integer(vec.size() - 1);
    }

    public static Object getObject(Integer i) 
    {
	return vec.get(i.intValue());
    }
    
}
