import streamit.*;

class IdentityLocal extends Filter {
    Channel input = new Channel(Float.TYPE, 1);
    Channel output = new Channel(Float.TYPE, 1);

    public void init() {}

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
    //    float weights[];
    int curr;
    int W;

    public Filter1(int N, int W) {
        super(N, W);
    }

    public void init(int N, int W) {
        int i;
        this.W = W;
        //this.weights = new float[W];
	//        for (i=0; i<W; i+=1)
	//            weights[i] = calcWeight(i, N, W);
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
                         2); //weights[curr++]);
        if(curr>= W) curr = 0;
    }
}

class Butterfly1 extends SplitJoin {
    public Butterfly1(int N, int W) { super (N, W); }

    public void init(final int N, final int W) {
        this.setSplitter(WEIGHTED_ROUND_ROBIN(N, N));
        this.add(new Filter1(N, W));
        this.add(new IdentityLocal());
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
        this.add(new IdentityLocal());
        this.add(new IdentityLocal());
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
        int i;
        this.setSplitter(WEIGHTED_ROUND_ROBIN((int)N/2, (int)N/2));
        for (i=0; i<2; i+=1)
            this.add(new SplitJoin2(N));
        this.setJoiner(ROUND_ROBIN());
    }
}

class FFTKernelLocal extends Pipeline {
    public FFTKernelLocal(int N)
    {
        super (N);
    }

    public void init(final int N) {
        int i;
        this.add(new SplitJoin1(N));
        for (i=2; i<N; i*=2) {
            this.add(new Butterfly1(i, N));
            this.add(new Butterfly2(i, N));
        }
    }
}

class OneSourceLocal extends Filter
{
    Channel output = new Channel(Float.TYPE, 1);
    public void initIO ()
    {
        this.streamOutput = output;
    }
    public void init () { }
    public void work()
    {
        output.pushFloat(1);
    }
}

class FloatPrinterLocal extends Filter
{
    Channel input = new Channel(Float.TYPE, 1);
    public void initIO ()
    {
        this.streamInput = input;
    }
    public void work ()
    {
        input.popFloat ();
    }
}

public class FFTLocal extends StreamIt {
    public static void main(String args[]) {
        new FFTLocal().run();
    }

    public void init() {
        this.add(new OneSourceLocal());
        this.add(new FFTKernelLocal(8192));
        this.add(new FloatPrinterLocal());
    }
}


