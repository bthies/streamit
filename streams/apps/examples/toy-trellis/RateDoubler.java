import streamit.library.*;
class RateDoubler extends Filter {
    public void init() {
	input = new Channel(Integer.TYPE, 1);
	output = new Channel(Integer.TYPE, 2);
    }
    public void work() {
	int temp;
	temp = input.popInt();
	output.pushInt(temp);
	output.pushInt(temp);
    }
}
