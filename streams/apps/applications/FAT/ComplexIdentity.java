/* This is the comlex delay class, this generates a delay in a complex stream. This structure will be later used
to generate a FIR filter */
import streamit.library.*;
class ComplexIdentity extends Filter {
                public void init() {
		    input = new Channel(new Complex().getClass(), 1);
		    output = new Channel(new Complex().getClass(), 1);
                }
                public void work() {
		    this.output.push(this.input.peek(0));
		    this.input.pop();
                }
}











