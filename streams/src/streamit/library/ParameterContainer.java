/*
 * Copyright 2003 by the Massachusetts Institute of Technology.
 *
 * Permission to use, copy, modify, and distribute this
 * software and its documentation for any purpose and without
 * fee is hereby granted, provided that the above copyright
 * notice appear in all copies and that both that copyright
 * notice and this permission notice appear in supporting
 * documentation, and that the name of M.I.T. not be used in
 * advertising or publicity pertaining to distribution of the
 * software without specific, written prior permission.
 * M.I.T. makes no representations about the suitability of
 * this software for any purpose.  It is provided "as is"
 * without express or implied warranty.
 */

package streamit.library;

import java.util.*;

import streamit.misc.AssertedClass;

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
        
        ParamData (char c)
        {
            data = new Character (c);
            primitive = true;
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
        
        ParamData (short s)
        {
            data = new Short (s);
            primitive = true;
        }
        
        int getInt ()
        {
            ASSERT (primitive);
            
            Integer intData = (Integer) data;
            ASSERT (intData != null);
            
            return intData.intValue ();
        }

        char getChar ()
        {
            ASSERT (primitive);
            
            Character charData = (Character) data;
            ASSERT (charData != null);
            
            return charData.charValue ();
        }

        float getFloat ()
        {
            ASSERT (primitive);
            
            Float floatData = (Float) data;
            ASSERT (floatData != null);
            
            return floatData.floatValue ();
        }

	Object getObject ()
        {
            ASSERT (!primitive);

            return data;            
        }

        boolean getBool ()
        {
            ASSERT (primitive);
            
            Boolean boolData = (Boolean) data;
            ASSERT (boolData != null);
            
            return boolData.booleanValue ();
        }
        
        short getShort ()
        {
            ASSERT (primitive);
            
            Short shortData = (Short) data;
            ASSERT (shortData != null);
            
            return shortData.shortValue ();
        }
        
        Object getObj ()
        {
            ASSERT (!primitive);
            
            return data;
        }
    }

    public ParameterContainer(String _paramName)
    {
        paramName = _paramName;
    }
    
    public ParameterContainer add (String paramName, Object obj)
    {
        ParamData data = new ParamData (obj);
        parameters.put (paramName, data);
        return this;
    }

    public ParameterContainer add (String paramName, char charParam)
    {
        ParamData data = new ParamData (charParam);
        parameters.put (paramName, data);
        return this;
    }
    
    public ParameterContainer add (String paramName, int intParam)
    {
        ParamData data = new ParamData (intParam);
        parameters.put (paramName, data);
        return this;
    }
    
    public ParameterContainer add (String paramName, float floatParam)
    {
        ParamData data = new ParamData (floatParam);
        parameters.put (paramName, data);
        return this;
    }
    
    public ParameterContainer add (String paramName, boolean boolParam)
    {
        ParamData data = new ParamData (boolParam);
        parameters.put (paramName, data);
        return this;
    }
    
    public ParameterContainer add (String paramName, short shortParam)
    {
        ParamData data = new ParamData (shortParam);
        parameters.put (paramName, data);
        return this;
    }
    
    public String getParamName () { return paramName; }
    
    public char getCharParam (String paramName)
    {
        ASSERT (parameters.containsKey (paramName));
        
        ParamData paramData = (ParamData) parameters.get (paramName);
        ASSERT (paramData != null);
        
        return paramData.getChar ();
    }

    public int getIntParam (String paramName)
    {
        ASSERT (parameters.containsKey (paramName));
        
        ParamData paramData = (ParamData) parameters.get (paramName);
        ASSERT (paramData != null);
        
        return paramData.getInt ();
    }

    public float getFloatParam (String paramName)
    {
        ASSERT (parameters.containsKey (paramName));
        
        ParamData paramData = (ParamData) parameters.get (paramName);
        ASSERT (paramData != null);
        
        return paramData.getFloat ();
    }

    public Object getObjectParam (String paramName)
    {
        ASSERT (parameters.containsKey (paramName));
        
        ParamData paramData = (ParamData) parameters.get (paramName);
        ASSERT (paramData != null);
        
        return paramData.getObject();
    }

    public boolean getBoolParam (String paramName)
    {
        ASSERT (parameters.containsKey (paramName));
        
        ParamData paramData = (ParamData) parameters.get (paramName);
        ASSERT (paramData != null);
        
        return paramData.getBool ();
    }

    public short getShortParam (String paramName)
    {
        ASSERT (parameters.containsKey (paramName));
        
        ParamData paramData = (ParamData) parameters.get (paramName);
        ASSERT (paramData != null);
        
        return paramData.getShort ();
    }

    public Object getObjParam (String paramName)
    {
        ASSERT (parameters.containsKey (paramName));
        
        ParamData paramData = (ParamData) parameters.get (paramName);
        ASSERT (paramData != null);
        
        return paramData.getObj ();
    }
    
    public String getStringParam (String paramName)
    {
        Object obj = getObjParam (paramName);
        String str = (String) obj;
        
        // make sure that either both or neither is null
        ASSERT (obj == null ^ str != null);
        
        return str;
    }
}
