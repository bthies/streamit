/* This is the comlex FIR class, this genereates an FIR filter for a complex stream.*/
import streamit.*;

class CompDelay extends FeedbackLoop {

    public CompDelay(int N) {
	super(N);
    }

    public void init(int N) {
	setSplitter(ROUND_ROBIN());
	setDelay(N);
	setLoop(new ComplexIdentity());
	setJoiner(ROUND_ROBIN());
    }

    public float initPathFloat(int index) {
	return 0.0f;
    }
}


  Filter {
                public void init() {
		    input = new Channel(new Complex().getClass(), 2);
		    output = new Channel(new Complex().getClass(), 2);
                }
                public void work() {
		    output.push(input.peek(1));
		     output.push(input.peek(0));
		    input.pop();
		    input.pop();
                }
	    });










