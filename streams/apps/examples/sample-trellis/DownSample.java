/**
 * Simple class that down samples
 * a stream by a factor of two. Throws error messages when there is an
 * two subsequent data items are not sequential.
 **/
import streamit.library.*;

class DownSample extends Filter {
    public void init() {
	input = new Channel(Integer.TYPE, 2); // pops 2
	output = new Channel(Integer.TYPE, 1); // pushes 1
    }
    public void work() {
	int temp1;
	int temp2;
	temp1 = input.popInt();
	temp2 = input.popInt();

	if (temp1 != temp2) {
	    System.out.println("Error decoding. Got " + temp1 + ", " + temp2);
	}
	
	output.pushInt(temp1);
    }
}
