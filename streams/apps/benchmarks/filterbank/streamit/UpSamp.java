import streamit.library.*;

class UpSamp extends Filter {

    public UpSamp(int N) {
	super(N);
    }
    int N;

    public void init(int N) {
		    input = new Channel(Float.TYPE, 1);
		    output = new Channel(Float.TYPE, N);
		    this.N=N;
                }

    public void work() {
		    output.pushFloat(this.input.popFloat());
		    for (int i=0;i<N-1;i++)
			output.pushFloat(0);
                }
	    };
    

  











