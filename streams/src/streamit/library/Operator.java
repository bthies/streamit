package streamit;

import java.lang.reflect.*;
import java.util.*;

// an operator takes N inputs and produces N outputs.
// Never explicitly instantiated
abstract class Operator extends DestroyedClass
{

    public static MessageStub MESSAGE_STUB;
    
    // initialize the MESSAGE_STUB
    {
        MESSAGE_STUB = MessageStub.STUB;
    }
    
    // allSinks is used for scheduling the streaming graph
    private static LinkedList allSinks;
    private static HashSet fullChannels;
    
    {
        allSinks = new LinkedList ();
        fullChannels = new HashSet ();
    }
    
    void AddSink ()
    {
        allSinks.add (this);
    }
    
    void RunSinks ()
    {
        ListIterator iter;
        iter = allSinks.listIterator ();
        
        // go over all the sinks
        while (iter.hasNext ())
        {
            Operator sink;
            sink = (Operator) iter.next ();
            ASSERT (sink != null);
            
            // do bunch of work
            int i;
            for (i = 0; i < 10; i++)
            {            
                sink.Work ();
            }
        }
    }
    
    void AddFullChannel (Channel channel)
    {
        fullChannels.add (channel);
    }

    // send a message to a handler that returns <stub> within <delay>
    // units of my input/output (to be specified more clearly...)
    public void SendMessage(MessageStub stub, int delay) {}

    // send a message to a handler that returns <stub> at the earliest
    // convenient time.
    public void SendMessage(MessageStub stub) {}

    protected static class MessageStub
    {
        private static MessageStub STUB = new MessageStub();
        private MessageStub() {}
    }
    
    // a prototype work function
    void Work () { }
    
    // ------------------------------------------------------------------
    // ------------------ all graph related functions -------------------
    // ------------------------------------------------------------------

    // get an IO field (input or output)
    // returns null if none present
    Channel[] GetIOFields (String fieldName)
    {
        ASSERT (fieldName == "input" || fieldName == "output");
        
        Channel fieldsInstance [] = null;
        
        try
        {
            Class thisClass = this.getClass ();
            ASSERT (thisClass != null);
            
            Field ioField;
            ioField  = thisClass.getField (fieldName);
            
            if ((Channel)ioField.get (this) != null)
            {
                fieldsInstance = new Channel [1];
                fieldsInstance [0] = (Channel) ioField.get (this);
                
                ASSERT (fieldsInstance [0] != null);
            } else {
                fieldsInstance = (Channel []) ioField.get (this);
                ASSERT (fieldsInstance != null);
            }
        }
        catch (NoSuchFieldException noError)
        {
            // do not do anything here, this is NOT an error!
        }
        catch (Throwable error)
        {
            // this is all the other errors:
            error.getClass ();
            ASSERT (false);
        }
        
        return fieldsInstance;
    }
    
    void SetIOField (String fieldName, int fieldIndex, Channel newChannel)
    {
        ASSERT (fieldName == "input" || fieldName == "output");
        
        Channel fieldsInstance [];
        
        try
        {
            Class thisClass = this.getClass ();
            ASSERT (thisClass != null);
            
            Field ioField;
            ioField  = thisClass.getField (fieldName);
            
            if (ioField.getType () == newChannel.getClass ())
            {
                ASSERT (fieldIndex == 0);
                ioField.set (this, newChannel);
            } else {
                fieldsInstance = (Channel []) ioField.get (this);
                ASSERT (fieldsInstance != null);
                ASSERT (fieldsInstance.length > fieldIndex);
                
                fieldsInstance [fieldIndex] = newChannel;
            }
            
        }
        catch (Throwable error)
        {
            // this is all the other errors:
            ASSERT (false);
        }
    }
}
