/*
parsing:
done parsing. outputing:
 <# pipeline <# -> void void #> FFT <# { <# add OneSource ( #> <# add FFTKernel <# ( 16 #> #> <# add FloatPrinter ( #> #> #> <# splitjoin <# -> float float #> FFTKernel <# ( <# N int #> #> <# { <# add <# splitjoin <# { split <# for <# i int <# = 0 #> #> <# < i 2 #> <# ++ i #> <# { <# add <# splitjoin <# { split add add join #> #> #> #> #> join #> #> #> <# for <# i int <# = 2 #> #> <# < i N #> <# { <# add Butterfly <# ( i N #> #> #> #> #> #> <# pipeline <# -> float float #> Butterfly <# ( <# N int #> <# W int #> #> <# { <# add <# splitjoin <# { split <# add Multiply ( #> add join #> #> #> <# add <# splitjoin <# { split <# add Subtract ( #> <# add Add ( #> join #> #> #> #> #> <# filter <# -> void float #> OneSource { <# work <# push 1 #> <# { <# push 1 #> #> #> #> <# filter <# -> float float #> Multiply { <# work <# push 1 #> <# pop 1 #> <# { <# push <# * 2 pop #> #> #> #> #> <# filter <# -> float float #> Add { <# work <# push 1 #> <# pop 2 #> <# { <# push <# + <# peek 0 #> <# peek 1 #> #> #> pop pop #> #> #> <# filter <# -> float float #> Subtract { <# work <# push 1 #> <# pop 2 #> <# { <# push <# - <# peek 0 #> <# peek 1 #> #> #> pop pop #> #> #> <# filter <# -> float void #> FloatPrinter { <# work <# pop 1 #> <# { <# $call <# . <# . System out #> println #> <# ( pop #> #> #> #> #> null
done outputing. walking:
*/
import streamit.*;
import streamit.io.*;

class Complex extends Structure {
  public double real, imag;
}

class FFT extends StreamIt
{
    static public void main (String [] args)
    {
        new FFT().run (args);
    }
    public void init() {
            add(new OneSource());
            add(new FFTKernel(16));
            add(new FloatPrinter());
        }
}

class FFTKernel extends SplitJoin
{
    private int N;
    public void init(final int N) {
        this.N = N;
            add(new SplitJoin() {
                public void init() {
                        setSplitter(null);
                        for (int i  = 0; (i < 2); ++i) {
                            add(new SplitJoin() {
                                public void init() {
                                        setSplitter(null);
                                        null;
                                        null;
                                        setJoiner(null);
                                    }
                            });
                        }
                        setJoiner(null);
                    }
            });
            for (int i  = 2; (i < N); ) 
        }
    public FFTKernel(final int N) {
        super(N);
    }
}

class Butterfly extends Pipeline
{
    private int N;
    private int W;
    public void init(final int N, final int W) {
        this.N = N;
        this.W = W;
            add(new SplitJoin() {
                public void init() {
                        setSplitter(null);
                        add(new Multiply());
                        null;
                        setJoiner(null);
                    }
            });
            add(new SplitJoin() {
                public void init() {
                        setSplitter(null);
                        add(new Subtract());
                        add(new Add());
                        setJoiner(null);
                    }
            });
        }
    public Butterfly(final int N, final int W) {
        super(N, W);
    }
}

class OneSource extends Filter
{
    public void work() {
        output.pushFloat(1);
    }
    public void init(final int N, final int W) {
        this.N = N;
        this.W = W;
        output = new Channel(Float.TYPE, 1);
    }
    public OneSource(final int N, final int W) {
        super(N, W);
    }
}

class Multiply extends Filter
{
    public void work() {
        output.pushFloat((2 * input.popFloat()));
    }
    public void init() {
        input = new Channel(Float.TYPE, 1);
        output = new Channel(Float.TYPE, 1);
    }
}

class Add extends Filter
{
    public void work() {
        output.pushFloat((input.peekFloat(0) + input.peekFloat(1)));
        input.popFloat();
        input.popFloat();
    }
    public void init() {
        input = new Channel(Float.TYPE, 2);
        output = new Channel(Float.TYPE, 1);
    }
}

class Subtract extends Filter
{
    public void work() {
        output.pushFloat((input.peekFloat(0) - input.peekFloat(1)));
        input.popFloat();
        input.popFloat();
    }
    public void init() {
        input = new Channel(Float.TYPE, 2);
        output = new Channel(Float.TYPE, 1);
    }
}

class FloatPrinter extends Filter
{
    public void work() {
        ;
    }
    public void init() {
        input = new Channel(Float.TYPE, 1);
    }
}

