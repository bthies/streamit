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
        
        ParamData (float f)
        {
            data = new Float (f);
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

        float GetFloat ()
        {
            ASSERT (primitive);
            
            Float floatData = (Float) data;
            ASSERT (floatData != null);
            
            return floatData.floatValue ();
        }

        boolean GetBool ()
        {
            ASSERT (primitive);
            
            Boolean boolData = (Boolean) data;
            ASSERT (boolData != null);
            
            return boolData.booleanValue ();
        }
        
        Object GetObj ()
        {
            ASSERT (!primitive);
            
            return data;
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
    
    public ParameterContainer Add (String paramName, float floatParam)
    {
        ParamData data = new ParamData (floatParam);
        parameters.put (paramName, data);
        return this;
    }
    
    public ParameterContainer Add (String paramName, boolean boolParam)
    {
        ParamData data = new ParamData (boolParam);
        parameters.put (paramName, data);
        return this;
    }
    
    public String GetParamName () { return paramName; }
    
    public int GetIntParam (String paramName)
    {
        ASSERT (parameters.containsKey (paramName));
        
        ParamData paramData = (ParamData) parameters.get (paramName);
        ASSERT (paramData != null);
        
        return paramData.GetInt ();
    }

    public float GetFloatParam (String paramName)
    {
        ASSERT (parameters.containsKey (paramName));
        
        ParamData paramData = (ParamData) parameters.get (paramName);
        ASSERT (paramData != null);
        
        return paramData.GetFloat ();
    }

    public boolean GetBoolParam (String paramName)
    {
        ASSERT (parameters.containsKey (paramName));
        
        ParamData paramData = (ParamData) parameters.get (paramName);
        ASSERT (paramData != null);
        
        return paramData.GetBool ();
    }

    public Object GetObjParam (String paramName)
    {
        ASSERT (parameters.containsKey (paramName));
        
        ParamData paramData = (ParamData) parameters.get (paramName);
        ASSERT (paramData != null);
        
        return paramData.GetObj ();
    }
    
    public String GetStringParam (String paramName)
    {
        Object obj = GetObjParam (paramName);
        String str = (String) obj;
        
        // make sure that either both or neither is null
        ASSERT (obj == null ^ str != null);
        
        return str;
    }
}
