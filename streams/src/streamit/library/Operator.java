package streamit;

// an operator takes N inputs and produces N outputs.  Never explicitly instantiated
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

}
