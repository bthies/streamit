// a channel is an I/O FIFO queue to go between filters

package streamit;

public class Channel {
    Class type;
    
    // the channel should be constructed with a 0-length array
    // indicating the type that will be held in this channel.
    public Channel(Object dataObject) 
    {
        type = dataObject.getClass ();
    }

    // PUSH OPERATIONS ----------------------------------------------

    // push something of type <type>
    public void Push(Object o) {}

    // push an int
    public void PushInt(int i) {}

    // push a char
    public void PushChar(char c) {}

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
    public char PopChar() { return 'x'; }

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

}
