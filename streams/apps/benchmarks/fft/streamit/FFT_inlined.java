import streamit.*;

class IdentityFloat extends Filter {
    public void init ()
    {
        input = new Channel(Float.TYPE, 1);
        output = new Channel(Float.TYPE, 1);
    }

    public void work() {
        output.pushFloat(input.popFloat());
    }
}

class Butterfly extends Pipeline {
    public Butterfly(int N, int W) { super (N, W); }

    public void init(final int N, final int W) {
        this.add(new SplitJoin() {
                public void init() {
                    this.setSplitter(WEIGHTED_ROUND_ROBIN(N, N));
                    this.add(new IdentityFloat());
                    this.add(new Filter() {
                            //float weights[] = new float[W];
                            int curr;

                            public void init() {
                                input = new Channel(Float.TYPE, 1);
                                output = new Channel(Float.TYPE, 1);
                                //for (int i=0; i<W; i++)
                                //    weights[i] = calcWeight(i, N, W);
                                curr = 0;
                            }

                            private float calcWeight(int a, int b, int c) {
                                return 1;
                            }

                            public void work() {
                                output.pushFloat(input.popFloat()*
                                                 2); //weights[curr++]);
                                if(curr>= W) curr = 0;
                            }

                        });
                    this.setJoiner(ROUND_ROBIN());
                }});

        this.add(new SplitJoin() {
                public void init() {
                    this.setSplitter(DUPLICATE());

                    this.add(new Filter() {
                            public void init ()
                            {
                                input = new Channel(Float.TYPE, 2);
                                output = new Channel(Float.TYPE, 1);
                            }

                            public void work() {
                                output.pushFloat(input.popFloat() +
                                                 input.popFloat());
                            }
                        });

                    this.add(new Filter() {
                            public void init ()
                            {
                                input = new Channel(Float.TYPE, 2);
                                output = new Channel(Float.TYPE, 1);
                            }

                            public void work() {
                                output.pushFloat(input.popFloat() -
                                                 input.popFloat());
                            }
                        });

                    this.setJoiner(WEIGHTED_ROUND_ROBIN(N, N));
                }});
    }}

class FFTKernel extends Pipeline {
    public FFTKernel(int N)
    {
        super (N);
    }

    public void init(final int N) {
        this.add(new SplitJoin() {
                public void init() {
                    this.setSplitter(WEIGHTED_ROUND_ROBIN((int)N/2, (int)N/2));
                    //for (int i=0; i<2; i++)
                        this.add(new SplitJoin() {
                                public void init() {
                                    this.setSplitter(ROUND_ROBIN());
                                    this.add(new IdentityFloat());
                                    this.add(new IdentityFloat());
                                    this.setJoiner(WEIGHTED_ROUND_ROBIN((int)
                                                                        N/4,
                                                                        (int)
                                                                        N/4));
                                }});
			this.add(new SplitJoin() {
                                public void init() {
                                    this.setSplitter(ROUND_ROBIN());
                                    this.add(new IdentityFloat());
                                    this.add(new IdentityFloat());
                                    this.setJoiner(WEIGHTED_ROUND_ROBIN((int)
                                                                        N/4,
                                                                        (int)
                                                                        N/4));
                                }});
                    this.setJoiner(ROUND_ROBIN());
                }});
        for (int i=1; i<N; i*=2)
            this.add(new Butterfly(i, N));
    }
}

class OneSource extends Filter
{
    public void init ()
    {
        output = new Channel(Float.TYPE, 1);
    }

    public void work()
    {
        output.pushFloat(1);
    }
}

class FloatPrinter extends Filter
{
    public void init ()
    {
        input = new Channel(Float.TYPE, 1);
    }
    public void work ()
    {
        System.out.println (input.popFloat ());
    }
}

public class FFT_inlined extends StreamIt {
    public static void main(String args[]) {
        new FFT_inlined().run(args);
    }

    public void init() {
        this.add(new OneSource());
        this.add(new FFTKernel(32));
        this.add(new FloatPrinter());
    }
}


