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
        
        // BUGBUG this should be an array, as should
        // input/output fields - unfortunately I don't
        // know how to compile that, and I don't know if
        // this would even be possible
        Channel fieldInstance []= new Channel [1];
        
        try
        {
            Class thisClass = this.getClass ();
            ASSERT (thisClass != null);
            
            Field ioField;
            ioField  = thisClass.getField (fieldName);
            
            fieldInstance [0] = (Channel) ioField.get (this);
            ASSERT (fieldInstance != null);
        }
        catch (NoSuchFieldException noError)
        {
            // do not do anything here, this is NOT an error!
        }
        catch (IllegalAccessException error)
        {
            // BUGBUG remove me when debugged
            error.getClass ();
            ASSERT (false);
        }
        catch (IllegalArgumentException error)
        {
            // BUGBUG remove me when debugged
            ASSERT (false);
        }
        catch (ExceptionInInitializerError error)
        {
            // BUGBUG remove me when debugged
            ASSERT (false);
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
