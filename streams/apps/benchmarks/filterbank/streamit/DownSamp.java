import streamit.library.*;

class DownSamp extends Filter {

    public DownSamp(int N) {
	super(N);
    }
    int N;

    public void init(int N) {
		    input = new Channel(Float.TYPE, N);
		    output = new Channel(Float.TYPE, 1);
		    this.N=N;
                }

    public void work() {
		    output.pushFloat(this.input.popFloat());
		    for (int i=0;i<N-1;i++)
			input.popFloat();
                }
	    };
    

  



