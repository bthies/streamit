import streamit.*;

public class SimpleSplit extends SplitJoin {
    public void init() {
	setSplitter(ROUND_ROBIN());
	add(new Filter() {
		Channel input = new Channel(Integer.TYPE, 1);
		Channel output = new Channel(Integer.TYPE, 1);
		public void work() {
		    output.pushInt(input.popInt());
		}
	    });
	setJoiner(ROUND_ROBIN());
    }
}
