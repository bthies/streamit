// a channel is an I/O FIFO queue to go between filters

package streamit;

import java.util.*;

public class Channel extends DestroyedClass 
{
    Class type;
    Operator source = null, sink = null;
    
    LinkedList queue;
    
    // the channel should be constructed with a 0-length array
    // indicating the type that will be held in this channel.
    public Channel(Object dataObject) 
    {
        ASSERT (dataObject.getClass ().isArray ());
        type = dataObject.getClass ().getComponentType ();
        queue = new LinkedList ();
    }
    
    void EnsureData ()
    {
        while (queue.isEmpty ())
        {
            ASSERT (source != null);
            
            source.Work ();
        }
    }

    // PUSH OPERATIONS ----------------------------------------------

    // push something of type <type>
    public void Push(Object o) {}

    // push an int
    public void PushInt(int i) {}

    // push a char
    public void PushChar(char c)
    {
        ASSERT (type.isPrimitive ());
        
        queue.addFirst (new Character  (c));
    }

    // push a double
    public void PushDouble(double d) {}

    // push a String
    public void PushString(String str) {}

    // POP OPERATIONS ----------------------------------------------

    // pop something of type <type>
    public Object Pop() { return null; }

    // pop an int
    public int PopInt() { return 0; }

    // pop a char
    public char PopChar()
    {
        ASSERT (type.isPrimitive ());
        
        EnsureData ();
        
        Character c;
        c = (Character) queue.removeLast ();
        ASSERT (c != null);
        
        return c.charValue ();
    }

    // pop a double
    public double PopDouble() { return 0; }

    // pop a String
    public String PopString() { return null; }

    // PEEK OPERATIONS ----------------------------------------------

    // peek at something of type <type>
    public Object Peek(int index) { return null; }

    // peek at an int
    public int PeekInt(int index) { return 0; }

    // peek at a char
    public char PeekChar(int index) { return 'x'; }

    // peek at a double
    public double PeekDouble(int index) { return 0; }

    // peek at a String
    public String PeekString(int index) { return null; }
    
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
}
