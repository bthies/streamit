package streamit;

// the basic stream class (pipe's).  has 1 input and 1 output.
public class Stream extends Operator {

    // CONSTRUCTORS --------------------------------------------------------------------

    public Stream() {
	init();
    }

    public Stream(int n) {
	init(n);
    }

    public Stream(String str) {
	init(str);
    }
    
    public Stream(Stream str) {
	init(str);
    }

    // INIT FUNCTIONS ---------------------------------------------------------------------
    
    // initializatoin functions, to be over-ridden
    public void init() {}

    // initializatoin functions, to be over-ridden
    public void init(int n) {}

    // initializatoin functions, to be over-ridden
    public void init(String str) {}

    // initializatoin functions, to be over-ridden
    public void init(Stream str) {}

    // RESET FUNCTIONS (need to just call init functions) ---------------------------------

    public MessageStub reset() {
	init();
	return MESSAGE_STUB;
    }

    public MessageStub reset(int n) {
	init(n);
	return MESSAGE_STUB;
    }

    public MessageStub reset(String str) {
	init(str);
	return MESSAGE_STUB;
    }

    public MessageStub reset(Stream str) {
	init(str);
	return MESSAGE_STUB;
    }

    // adds something to the pipeline
    public Stream add(Stream s) { return s; }

    // just a runtime hook to run the stream
    public void run() {};
}



