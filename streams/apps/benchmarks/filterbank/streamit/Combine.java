import streamit.library.*;

class Combine extends Filter {

    public Combine(int N) {
	super(N);
    }
    int N;

    public void init(int N) {
		    input = new Channel(Float.TYPE, N);
		    output = new Channel(Float.TYPE, 1);
		    this.N=N;
                }

    public void work() {
	
	float sum=0;
	for (int i=0;i<N;i++)
	    sum+=input.popFloat();

	output.pushFloat(sum);
		   
	
                }
	    };
    

  











