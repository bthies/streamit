import streamit.library.*;

class OneToOne extends Filter {
    public void init() {
	input = new Channel(Integer.TYPE, 1);
	output = new Channel(Integer.TYPE, 1);
    }
    public void work() {
	output.pushInt(input.popInt());
    }
}
