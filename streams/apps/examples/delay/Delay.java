import streamit.library.*;

class Delay extends FeedbackLoop {

    public Delay(int N) {
	super(N);
    }

    public void init(int N) {
	setSplitter(ROUND_ROBIN());
	setDelay(N);
	setBody(new Filter() {
                public void init() {
		    input = new Channel(Float.TYPE, 2);
		    output = new Channel(Float.TYPE, 2);
                }
                public void work() {
		    this.output.pushFloat(this.input.peekFloat(1));
		    this.output.pushFloat(this.input.peekFloat(0));
		    this.input.popFloat();
		    this.input.popFloat();
                }
	    });
	setLoop(new FloatIdentity());
	setJoiner(ROUND_ROBIN());
    }

    public float initPathFloat(int index) {
	return 0.0f;
    }
}
