package streamit;

// an operator takes N inputs and produces N outputs.  Never explicitly instantiated
abstract class Operator {

    public static MessageStub MESSAGE_STUB = MessageStub.STUB;

    // send a message to a handler that returns <stub> within <delay>
    // units of my input/output (to be specified more clearly...)
    public void sendMessage(MessageStub stub, int delay) {}

    // send a message to a handler that returns <stub> at the earliest
    // convenient time.
    public void sendMessage(MessageStub stub) {}

    protected static class MessageStub {
	private static MessageStub STUB = new MessageStub();
	private MessageStub() {}
    }

}
