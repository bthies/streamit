package streamit;

import java.lang.reflect.*;

// an operator takes N inputs and produces N outputs.
// Never explicitly instantiated
abstract class Operator extends DestroyedClass
{

    public static MessageStub MESSAGE_STUB;
    
    // initialize the MESSAGE_STUB
    {
        MESSAGE_STUB = MessageStub.STUB;
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
    
    // ------------------------------------------------------------------
    // ------------------ all graph related functions -------------------
    // ------------------------------------------------------------------

    // get an IO field (input or output)
    // returns null if none present
    Channel[] GetIOFields (String fieldName)
    {
        ASSERT (fieldName == "input" || fieldName == "output");
        
        Channel fieldInstance[] = null;
        
        try
        {
            Class thisClass = this.getClass ();
            ASSERT (thisClass != null);
            
            Field fields;
            fields  = thisClass.getField (fieldName);
            
            fieldInstance = (Channel[]) fields.get (this);
            ASSERT (fieldInstance != null);
        }
        catch (NoSuchFieldException noError)
        {
            // do not do anything here, this is NOT an error!
        }
        catch (Throwable error)
        {
            // this is all the other errors:
            ASSERT (false);
        }
        
        return fieldInstance;
    }
    
    void SetIOField (String fieldName, int fieldIndex, Channel newChannel)
    {
        ASSERT (fieldIndex >= 0);
        
        Channel fieldInstance[];
        fieldInstance = GetIOFields (fieldName);
        
        ASSERT (fieldInstance != null && fieldInstance.length < fieldIndex);
        fieldInstance [0] = newChannel;
    }    
}
