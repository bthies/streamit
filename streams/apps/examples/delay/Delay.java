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
		    this.output.pushInt(this.input.peekInt(1));
		    this.output.pushInt(this.input.peekInt(0));
		    this.input.pop();
		    this.input.pop();
                }
	    });
	setJoiner(ROUND_ROBIN());
    }

    public int initPathInt(int index) {
	return 0;
    }
}
