import streamit.library.*;

/** Input sink for the stream graph containers **/
public class InputSink extends Filter {
    public void init() {
	input = new Channel(Integer.TYPE, 1); // push 1 int
    }
    public void work() {
	System.out.println(input.popInt());
    }
}
