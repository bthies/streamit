import streamit.*;

class Delay extends FeedbackLoop {

    public Delay(int N) {
	super(N);
    }

    public void init(int N) {
	setSplitter(ROUND_ROBIN());
	setDelay(N);
	setBody(new Filter() {
                public void init() {
		    setInput(Float.TYPE);setOutput(Float.TYPE);
		    setPush(2); setPop(2);
                }
                public void work() {
		    this.output.pushFloat(this.input.peekFloat(1));
		    this.output.pushFloat(this.input.peekFloat(0));
		    this.input.popFloat();
		    this.input.popFloat();
                }
	    });
	setJoiner(ROUND_ROBIN());
    }

    public float initPathFloat(int index) {
	return 0.0f;
    }
}
