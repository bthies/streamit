import streamit.*;

class Identity extends Filter {
    Channel input = new Channel(Float.TYPE, 1);
    Channel output = new Channel(Float.TYPE, 1);

    public void initIO() {
        this.streamInput = input;
        this.streamOutput = output;
    }

    public void work() {
        output.pushFloat(input.popFloat());
    }
}

class Filter1 extends Filter {
    
    Channel input = new Channel(Float.TYPE, 1);
    Channel output = new Channel(Float.TYPE, 1);
    float weights[];
    int curr;
    int W;

    public Filter1(int N, int W) {
	super(N, W);
    }
    
    public void init(int N, int W) {
	this.W = W;
	this.weights = new float[W];
	for (int i=0; i<W; i++)
	    weights[i] = calcWeight(i, N, W);
	curr = 0;
    }
    
    public void initIO() {
	this.streamInput = input;
	this.streamOutput = output;
    }
    
    private float calcWeight(int a, int b, int c) {
	return 1;
    }
    
    public void work() {
	output.pushFloat(input.popFloat()*
			 weights[curr++]);
	if(curr>= W) curr = 0;
    }
}

class Butterfly1 extends SplitJoin {
    public Butterfly1(int N, int W) { super (N, W); }

    public void init(final int N, final int W) {
	this.setSplitter(WEIGHTED_ROUND_ROBIN(N, N));
	this.add(new Filter1(N, W));
	this.add(new Identity());
	this.setJoiner(ROUND_ROBIN());
    }
}

class Butterfly2 extends SplitJoin {
    public Butterfly2(int N, int W) { super (N, W); }

    public void init(final int N, final int W) {
	this.setSplitter(DUPLICATE());
    
	this.add(new Filter() {
		Channel input = new Channel(Float.TYPE, 2);
		Channel output = new Channel(Float.TYPE, 1);
		
		public void initIO ()
		{
		    this.streamInput = input;
		    this.streamOutput = output;
		}
		
		public void work() {
		    output.pushFloat(input.popFloat() +
				     input.popFloat());
		}
	    });
	this.add(new Filter() {
		Channel input = new Channel(Float.TYPE, 2);
		Channel output = new Channel(Float.TYPE, 1);
		
		public void initIO ()
		{
		    this.streamInput = input;
		    this.streamOutput = output;
		}
		
		public void work() {
		    output.pushFloat(input.popFloat() -
				     input.popFloat());
		}
	    });
	
	this.setJoiner(WEIGHTED_ROUND_ROBIN(N, N));
    }
}

class SplitJoin2 extends SplitJoin {
    public SplitJoin2(int N) {
	super(N);
    }

    public void init(int N) {
	this.setSplitter(ROUND_ROBIN());
	this.add(new Identity());
	this.add(new Identity());
	this.setJoiner(WEIGHTED_ROUND_ROBIN((int)
					    N/4,
					    (int)
					    N/4));
    }
}

class SplitJoin1 extends SplitJoin {
    public SplitJoin1(int N) {
	super(N);
    }

    public void init(int N) {
	this.setSplitter(WEIGHTED_ROUND_ROBIN((int)N/2, (int)N/2));
	for (int i=0; i<2; i++)
	    this.add(new SplitJoin2(N));
	this.setJoiner(ROUND_ROBIN());
    }
}

class FFTKernel extends Pipeline {
    public FFTKernel(int N)
    {
        super (N);
    }

    public void init(final int N) {
        this.add(new SplitJoin1(N));
        for (int i=2; i<N; i*=2) {
            this.add(new Butterfly1(i, N));
            this.add(new Butterfly2(i, N));
	}
    }
}

class OneSource extends Filter
{
    Channel output = new Channel(Float.TYPE, 1);
    public void initIO ()
    {
        this.streamOutput = output;
    }
    public void work()
    {
        output.pushFloat(1);
    }
}

class FloatPrinter extends Filter
{
    Channel input = new Channel(Float.TYPE, 1);
    public void initIO ()
    {
        this.streamInput = input;
    }
    public void work ()
    {
        System.out.println (input.popFloat ());
    }
}

public class FFTLocal extends StreamIt {
    public static void main(String args[]) {
        new FFT().run();
    }

    public void init() {
        this.add(new OneSource());
        this.add(new FFTKernel(16));
        this.add(new FloatPrinter());
    }
}


