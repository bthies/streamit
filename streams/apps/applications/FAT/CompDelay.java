/* This is the comlex delay class, this generates a delay in a complex stream. This structure will be later used
to generate a FIR filter */
import streamit.library.*;

class CompDelay extends FeedbackLoop {

    public CompDelay(int N) {
	super(N);
    }

    public void init(int N) {
	setSplitter(ROUND_ROBIN());
	setDelay(N);
	setBody(new Filter() {
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
	setLoop(new ComplexIdentity());
	setJoiner(ROUND_ROBIN());
    }

    public float initPathFloat(int index) {
	return 0.0f;
    }
}












