package streamit;

import java.util.*;

// the basic stream class (pipe's).  has 1 input and 1 output.
public class Stream extends Operator {

    // CONSTRUCTORS --------------------------------------------------------------------

    List streamElements;
    
    public Stream() 
    {
        StreamInit ();
	Init();
    }

    public Stream(int n) 
    {
        StreamInit ();
	Init(n);
    }

    public Stream(String str) 
    {
        StreamInit ();
	Init(str);
    }
    
    public Stream(Stream str) 
    {
        StreamInit ();
	Init(str);
    }

    // INIT FUNCTIONS ---------------------------------------------------------------------
    
    // initializatoin functions, to be over-ridden
    public void Init() {}

    // initializatoin functions, to be over-ridden
    public void Init(int n) {}

    // initializatoin functions, to be over-ridden
    public void Init(String str) {}

    // initializatoin functions, to be over-ridden
    public void Init(Stream str) {}
    
    // general initialization function for Stream class only
    
    private void StreamInit ()
    {
        streamElements = new LinkedList ();
    }

    // RESET FUNCTIONS (need to just call init functions) ---------------------------------

    public MessageStub Reset() 
    {
	Init();
	return MESSAGE_STUB;
    }

    public MessageStub Reset(int n) 
    {
	Init(n);
	return MESSAGE_STUB;
    }

    public MessageStub Reset(String str)
    {
	Init(str);
	return MESSAGE_STUB;
    }

    public MessageStub Reset(Stream str)
    {
	Init(str);
	return MESSAGE_STUB;
    }

    // adds something to the pipeline
    public Stream Add(Stream s) { return s; }

    // just a runtime hook to run the stream
    public void Run() {}
}



