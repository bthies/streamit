import streamit.library.*;

/** Character Unit delay **/
class Delay_N extends Filter {
    float[] state;
    int N;
    int place_holder;

    public Delay_N(int N) {
	super(N);
	    }

    public void init(int N) {
	// initial state of delay is 0
	state=new float[N];
	this.N=N;
	for (int i=0; i<N; i++)
	    state[i]=0;
	input = new Channel(Float.TYPE,1);
	output = new Channel(Float.TYPE,1);
	place_holder=0;
    }
    public void work() {
	// push out the state and then update it with the input
	// from the channel
	output.pushFloat(state[place_holder]);
	state[place_holder] = input.popFloat();
	place_holder++;
	if (place_holder==N)
	    place_holder=0;	
    }
}




