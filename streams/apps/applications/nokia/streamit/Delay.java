/**
 * Simple parameterized delay filter.
 **/
import streamit.library.*;

public class Delay extends Pipeline {
    public Delay(int delay) {
	super(delay);
    }
    public void init(int delay) {
	// basically, just add a bunch of unit delay filters
	for (int i=0; i<delay; i++) {
	    this.add(new Delay_one());
	}
    }
}



/** Character Unit delay **/
class Delay_one extends Filter {
    float state;
    public void init() {
	// initial state of delay is 0
	this.state = 0;
	input = new Channel(Float.TYPE,1);
	output = new Channel(Float.TYPE,1);
    }
    public void work() {
	// push out the state and then update it with the input
	// from the channel
	output.pushFloat(this.state);
	this.state = input.popFloat();
    }
}
