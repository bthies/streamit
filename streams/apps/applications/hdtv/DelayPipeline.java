/**
 * Simple parameterized delay filter.
 **/
import streamit.library.*;

public class DelayPipeline extends Pipeline {
    public DelayPipeline(int delay) {
	super(delay);
    }
    public void init(int delay) {
	// basically, just add a bunch of unit delay filters
	for (int i=0; i<delay; i++) {
	    this.add(new Delay());
	}
    }
}



/** Character Unit delay **/
class Delay extends Filter {
    int state;
    public void init() {
	// initial state of delay is 0
	this.state = 0;
	input = new Channel(Integer.TYPE,1);
	output = new Channel(Integer.TYPE,1);
    }
    public void work() {
	// push out the state and then update it with the input
	// from the channel
	output.pushInt(this.state);
	this.state = input.popInt();
    }
}
