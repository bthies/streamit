// a channel is an I/O FIFO queue to go between filters

package streamit;

import java.util.*;
import java.lang.*;

public class Channel extends DestroyedClass implements Cloneable
{
    Class type;
    Operator source = null, sink = null;
    boolean declaredFull = false;
    
    LinkedList queue;
    
    // the channel should be constructed with a 0-length array
    // indicating the type that will be held in this channel.
    public Channel(Object dataObject) 
    {
        ASSERT (dataObject.getClass ().isArray ());
        type = dataObject.getClass ().getComponentType ();
        queue = new LinkedList ();
    }
    
    void EnsureData (int amount)
    {
        while (queue.size () < amount)
        {
            ASSERT (source != null);
            
            source.Work ();
        }
    }
    
    void EnsureData ()
    {
        EnsureData (1);
    }
    
    private void Enqueue (Object o)
    {
    	queue.addLast (o);
    	
    	// overflow at 50 chars in the queue
    	if (queue.size () > 100 && !declaredFull)
    	{
    		source.AddFullChannel (this);
            declaredFull = true;
    	}
    }
    
    private Object Dequeue ()
    {
    	if (queue.size () < 50 && declaredFull)
    	{
    		source.RemoveFullChannel (this);
            declaredFull = false;
    	}
    	
    	return queue.removeFirst ();
    }
    

    // PUSH OPERATIONS ----------------------------------------------

    // push something of type <type>
    public void Push(Object o)
    {
        ASSERT (o.getClass () == type);
        
        Enqueue (o);
    }

    // push an int
    public void PushInt(int i) 
    {
        ASSERT (type == Integer.TYPE);
        
        Enqueue (new Integer (i));
    }

    // push a char
    public void PushChar(char c)
    {
        ASSERT (type == Character.TYPE);
        
        Enqueue (new Character  (c));
    }

    // push a double
    public void PushDouble(double d)
    {
        ASSERT (type == Double.TYPE);
        
        Enqueue (new Double (d));
    }

    // push a String
    public void PushString(String str)
    {
        Push (str);
    }

    // POP OPERATIONS ----------------------------------------------

    // pop something of type <type>
    public Object Pop()
    {
        EnsureData ();
        
        Object data;
        data = Dequeue ();
        ASSERT (data != null);
        
        return data;
    }

    // pop an int
    public int PopInt()
    {
        ASSERT (type == Integer.TYPE);
        
        Integer data;
        data = (Integer) Pop ();
        ASSERT (data != null);
        
        return data.intValue ();
    }
    

    // pop a char
    public char PopChar()
    {
        ASSERT (type == Character.TYPE);
        
        Character c;
        c = (Character) Pop ();
        ASSERT (c != null);
        
        return c.charValue ();
    }

    // pop a double
    public double PopDouble()
    {
        ASSERT (type == Double.TYPE);
        
        Double data;
        data = (Double) Pop ();
        ASSERT (data != null);
        
        return data.doubleValue ();
    }
    

    // pop a String
    public String PopString() 
    {
        String data = (String) Pop ();;
        ASSERT (data != null);

        return data;
    }

    // PEEK OPERATIONS ----------------------------------------------

    // peek at something of type <type>
    public Object Peek(int index)
    {
        EnsureData (index);
        
        Object data;
        data = queue.get (index - 1);
        ASSERT (data != null);
        
        return data;
    }

    // peek at an int
    public int PeekInt(int index)
    {
        ASSERT (type == Integer.TYPE);
        
        Integer data;
        data = (Integer) Peek (index);
        ASSERT (data != null);
        
        return data.intValue ();
    }

    // peek at a char
    public char PeekChar(int index)
    {
        ASSERT (type == Character.TYPE);
        
        Character data;
        data = (Character) Peek (index);
        ASSERT (data != null);
        
        return data.charValue ();
    }

    // peek at a double
    public double PeekDouble(int index)
    {
        ASSERT (type == Double.TYPE);
        
        Double data;
        data = (Double) Peek (index);
        ASSERT (data != null);
        
        return data.doubleValue ();
    }

    // peek at a String
    public String PeekString(int index)
    {
        String data;
        data = (String) Peek (index);
        ASSERT (data != null);
        
        return data;
    }
    
    // ------------------------------------------------------------------
    //                  syntax checking functions
    // ------------------------------------------------------------------
    
    Class GetType () { return type; }

    // ------------------------------------------------------------------
    //                  graph keeping functions
    // ------------------------------------------------------------------

    Operator GetSource () { return source; }
    Operator GetSink () { return sink; }
    
    void SetSource (Operator _source) { source = _source; }
    void SetSink (Operator _sink) { sink = _sink; }
    
    protected Object clone ()
    {
        Channel result = null;
        try 
        {
            result = (Channel) super.clone ();
            ASSERT (result != null);
        }
        catch (Throwable error)
        {
            // this should NOT happen!
            ASSERT (false);
        }
        
        return result;
    }
}
