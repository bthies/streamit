import streamit.library.*;

/** Input source for the stream graph containers **/
public class InputSource extends Filter {
    public void init() {
	output = new Channel(Integer.TYPE, 1); // push 1 int
    }
    public void work() {
	output.pushInt(0);
    }
}
