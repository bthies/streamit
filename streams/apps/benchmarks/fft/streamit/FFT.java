import streamit.*;

class Identity extends Filter {
    Channel input = new Channel(Float.TYPE, 1);
    Channel output = new Channel(Float.TYPE, 1);

    public void initIO() {
        streamInput = input;
        streamOutput = output;
    }

    public void work() {
        output.pushFloat(input.popFloat());
    }
}

class Butterfly extends Pipeline {
    public Butterfly(int N, int W) { super (N, W); }

    public void init(final int N, final int W) {
        add(new SplitJoin() {
                public void init() {
                    setSplitter(WEIGHTED_ROUND_ROBIN(N, N));
                    add(new Filter() {
                            Channel input = new Channel(Float.TYPE, 1);
                            Channel output = new Channel(Float.TYPE, 1);
                            float weights[] = new float[W];
                            int curr;

                            public void init() {
                                for (int i=0; i<W; i++)
                                    weights[i] = calcWeight(i, N, W);
                                curr = 0;
                            }

                            public void initIO() {
                                streamInput = input;
                                streamOutput = output;
                            }

                            private float calcWeight(int a, int b, int c) {
                                return 1;
                            }

                            public void work() {
                                output.pushFloat(input.popFloat()*
                                                 weights[curr++]);
                                if(curr>= W) curr = 0;
                            }

                        });
                    add(new Identity());
                    setJoiner(ROUND_ROBIN());
                }});

        add(new SplitJoin() {
                public void init() {
                    setSplitter(DUPLICATE());

                    add(new Filter() {
                            Channel input = new Channel(Float.TYPE, 2);
                            Channel output = new Channel(Float.TYPE, 1);

                            public void initIO ()
                            {
                                streamInput = input;
                                streamOutput = output;
                            }

                            public void work() {
                                output.pushFloat(input.popFloat() -
                                                 input.popFloat());
                            }
                        });

                    add(new Filter() {
                            Channel input = new Channel(Float.TYPE, 2);
                            Channel output = new Channel(Float.TYPE, 1);

                            public void initIO ()
                            {
                                streamInput = input;
                                streamOutput = output;
                            }

                            public void work() {
                                output.pushFloat(input.popFloat() +
                                                 input.popFloat());
                            }
                        });
                    setJoiner(WEIGHTED_ROUND_ROBIN(N, N));
                }});
    }}

class FFTKernel extends Pipeline {
    public FFTKernel(int N)
    {
        super (N);
    }

    public void init(final int N) {
        add(new SplitJoin() {
                public void init() {
                    setSplitter(WEIGHTED_ROUND_ROBIN((int)N/2, (int)N/2));
                    for (int i=0; i<2; i++)
                        add(new SplitJoin() {
                                public void init() {
                                    setSplitter(ROUND_ROBIN());
                                    add(new Identity());
                                    add(new Identity());
                                    setJoiner(WEIGHTED_ROUND_ROBIN((int)
                                                                        N/4,
                                                                        (int)
                                                                        N/4));
                                }});
                    setJoiner(ROUND_ROBIN());
                }});
        for (int i=2; i<N; i*=2)
            add(new Butterfly(i, N));
    }
}

class OneSource extends Filter
{
    Channel output = new Channel(Float.TYPE, 1);
    public void initIO ()
    {
        streamOutput = output;
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
        streamInput = input;
    }
    public void work ()
    {
        System.out.println (input.popFloat ());
    }
}

public class FFT extends StreamIt {
    public static void main(String args[]) {
        new FFT().run();
    }

    public void init() {
        add(new OneSource());
        add(new FFTKernel(16));
        add(new FloatPrinter());
    }
}


