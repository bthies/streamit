package streamit;

import java.util.*;

public class ParameterContainer extends AssertedClass
{
    Map parameters = new TreeMap ();
    String paramName;
    
    class ParamData
    {
        Object data;
        boolean primitive;
        
        ParamData (Object d)
        {
            data = d;
            primitive = false;
        }
        
        ParamData (int d)
        {
            data = new Integer (d);
            primitive = true;
        }
        
        ParamData (boolean d)
        {
            data = new Boolean (d);
            primitive = true;
        }
        
        int GetInt ()
        {
            ASSERT (primitive);
            
            Integer intData = (Integer) data;
            ASSERT (intData != null);
            
            return intData.intValue ();
        }

        boolean GetBool ()
        {
            ASSERT (primitive);
            
            Boolean boolData = (Boolean) data;
            ASSERT (boolData != null);
            
            return boolData.booleanValue ();
        }
    }

    public ParameterContainer(String _paramName)
    {
        paramName = _paramName;
    }
    
    public ParameterContainer Add (String paramName, Object obj)
    {
        ParamData data = new ParamData (obj);
        parameters.put (paramName, data);
        return this;
    }

    public ParameterContainer Add (String paramName, int intParam)
    {
        ParamData data = new ParamData (intParam);
        parameters.put (paramName, data);
        return this;
    }
    
    public ParameterContainer Add (String paramName, boolean boolParam)
    {
        ParamData data = new ParamData (boolParam);
        parameters.put (paramName, data);
        return this;
    }
    
    public int GetIntParam (String paramName)
    {
        ASSERT (parameters.containsKey (paramName));
        
        ParamData paramData = (ParamData) parameters.get (paramName);
        ASSERT (paramData != null);
        
        return paramData.GetInt ();
    }

    public boolean GetBoolParam (String paramName)
    {
        ASSERT (parameters.containsKey (paramName));
        
        ParamData paramData = (ParamData) parameters.get (paramName);
        ASSERT (paramData != null);
        
        return paramData.GetBool ();
    }
}
