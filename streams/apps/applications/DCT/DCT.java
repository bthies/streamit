/*
parsing:
done parsing. outputing:
 <# pipeline <# -> void void #> DCT ( <# { <# add source ( #> <# add DCTcore <# ( 4 #> #> <# add sink <# ( 4 #> #> #> #> <# filter <# -> void float #> source ( { <# test <# [ float 4 #> #> <# init <# { <# = <# $array test <# [ 0 #> #> 1 #> <# = <# $array test <# [ 1 #> #> 1 #> <# = <# $array test <# [ 2 #> #> 1 #> <# = <# $array test <# [ 3 #> #> 1 #> #> #> <# work <# push 2 #> <# { <# push <# $array test <# [ 0 #> #> #> <# push <# $array test <# [ 1 #> #> #> #> #> #> <# filter <# -> float void #> sink <# ( <# N int #> #> { <# work <# pop N #> <# { <# for <# i int <# = 0 #> #> <# < i N #> <#  i #> <# $call println <# ( pop #> #> #> #> #> #> <# pipeline <# -> float float #> DCTcore <# ( <# N int #> #> <# { <# if <# == N 2 #> <# { <# add TwoPointDCT ( #> #> <# { <# add bitrev <# ( N #> #> <# add recurse <# ( N #> #> #> #> #> #> <# splitjoin <# -> float float #> recurse <# ( <# N int #> #> <# { <# split roundrobin <# ( <# / N 2 #> #> #> <# add DCTcore <# ( <# / N 2 #> #> #> <# add reordDCT <# ( <# / N 2 #> #> #> <# join roundrobin <# ( 1 #> #> #> #> <# pipeline <# -> float float #> reordDCT <# ( <# N int #> #> <# { <# add DCTcore <# ( N #> #> <# add outmix <# ( N #> #> #> #> <# filter <# -> float float #> outmix <# ( <# N int #> #> { <# work <# push N #> <# pop N #> <# { <# in_arr <# [ float N #> #> <# for <# i int <# = 0 #> #> <# < i N #> <#  i #> <# = <# $array in_arr <# [ i #> #> pop #> #> <# for <# i int <# = 0 #> #> <# < i <# - N 1 #> #> <#  i #> <# push <# + <# $array in_arr <# [ i #> #> <# $array in_arr <# [ <# + i 1 #> #> #> #> #> #> <# push <# $array in_arr <# [ <# - N 1 #> #> #> #> #> #> #> <# filter <# -> float float #> TwoPointDCT ( { <# scale float #> <# init <# { <# = scale <# $call sqrt <# ( 2 #> #> #> #> #> <# work <# push 2 #> <# pop 2 #> <# { <# a float #> <# b float #> <# = a pop #> <# = b pop #> <# push <# / <# + a b #> scale #> #> <# push <# / <# - a b #> scale #> #> #> #> #> <# filter <# -> float float #> decimate <# ( <# N int #> #> { <# coef <# [ float <# / N 2 #> #> #> <# init <# { <# for <# i int <# = 0 #> #> <# < i <# / N 2 #> #> <#  i #> <# = <# $array coef <# [ i #> #> <# / 1 <# * 2 <# * <# $call sqrt <# ( <# / 2 N #> #> #> <# $call cos <# ( <# * <# + <# * 2 i #> 1 #> <# / 3.141592653589793 <# * 2 N #> #> #> #> #> #> #> #> #> #> #> #> <# work <# push N #> <# pop N #> <# { <# in_arr <# [ float N #> #> <# for <# i int <# = 0 #> #> <# < i N #> <#  i #> <# = <# $array in_arr <# [ i #> #> pop #> #> <# for <# i int <# = 0 #> #> <# < i <# / N 2 #> #> <#  i #> <# push <# + <# $array in_arr <# [ i #> #> <# $array in_arr <# [ <# + i <# / N 2 #> #> #> #> #> #> #> <# temp float #> <# for <# i int <# = 0 #> #> <# < i <# / N 2 #> #> <#  i #> <# { <# = temp <# - <# $array in_arr <# [ i #> #> <# $array in_arr <# [ <# + i <# / N 2 #> #> #> #> #> #> <# push <# * temp <# $array coef <# [ i #> #> #> #> #> #> #> #> #> <# splitjoin <# -> float float #> bitrev <# ( <# N int #> #> <# { <# split roundrobin <# ( <# / N 2 #> #> #> <# add DamnIdentity ( #> <# add reversal <# ( <# / N 2 #> #> #> <# join roundrobin <# ( <# / N 2 #> #> #> #> #> <# filter <# -> float float #> DamnIdentity ( { <# work <# push 1 #> <# pop 1 #> <# { <# push pop #> #> #> #> <# filter <# -> float float #> reversal <# ( <# N int #> #> { <# work <# push N #> <# pop N #> <# peek N #> <# { <# for <# i int <# = <# - N 1 #> #> #> <# >= i 0 #> <#  i #> <# push <# peek i #> #> #> <# for <# i int <# = 0 #> #> <# < i N #> <#  i #> pop #> #> #> #> null
done outputing. walking:
*/
import streamit.*;
import streamit.io.*;

class Complex extends Structure {
  public double real, imag;
}

class DCT extends StreamIt
{
    static public void main (String [] args)
    {
        new DCT().run (args);
    }
    public void init() {
            add(new source());
            add(new DCTcore(4));
            add(new sink(4));
        }
    public DCT() {
        super();
    }
}

class source extends Filter
{
    private float[] test;
    public void work() {
        output.pushFloat(test[0]);
        output.pushFloat(test[1]);
    }
    public void init()     {
        this.test = new float[4];
        output = new Channel(Float.TYPE, 2);
        test[0] = 1;
        test[1] = 1;
        test[2] = 1;
        test[3] = 1;
    }
    public source() {
        super();
    }
}

class sink extends Filter
{
    private int N;
    public void work() {
        for (int i  = 0; (i < N); i++) {System.out.println(input.popFloat());}
    }
    public void init(final int N) {
        this.N = N;
        input = new Channel(Float.TYPE, N);
    }
    public sink(final int N) {
        super(N);
    }
}

class DCTcore extends Pipeline
{
    private int N;
    public void init(final int N) {
        this.N = N;
            if ((N == 2)) {{
                add(new TwoPointDCT());
            }} else {{
                add(new bitrev(N));
                add(new recurse(N));
            }}
        }
    public DCTcore(final int N) {
        super(N);
    }
}

class recurse extends SplitJoin
{
    private int N;
    public void init(final int N) {
        this.N = N;
            setSplitter(ROUND_ROBIN((N / 2)));
            add(new DCTcore((N / 2)));
            add(new reordDCT((N / 2)));
            setJoiner(ROUND_ROBIN(1));
        }
    public recurse(final int N) {
        super(N);
    }
}

class reordDCT extends Pipeline
{
    private int N;
    public void init(final int N) {
        this.N = N;
            add(new DCTcore(N));
            add(new outmix(N));
        }
    public reordDCT(final int N) {
        super(N);
    }
}

class outmix extends Filter
{
    private int N;
    public void work() {
        float[] in_arr = new float[N];
        for (int i  = 0; (i < N); i++) {in_arr[i] = input.popFloat();}
        for (int i  = 0; (i < (N - 1)); i++) {output.pushFloat((in_arr[i] + in_arr[(i + 1)]));}
        output.pushFloat(in_arr[(N - 1)]);
    }
    public void init(final int N) {
        this.N = N;
        input = new Channel(Float.TYPE, N);
        output = new Channel(Float.TYPE, N);
    }
    public outmix(final int N) {
        super(N);
    }
}

class TwoPointDCT extends Filter
{
    private float scale;
    public void work() {
        float a ;
        float b ;
        a = input.popFloat();
        b = input.popFloat();
        output.pushFloat(((a + b) / scale));
        output.pushFloat(((a - b) / scale));
    }
    public void init()     {
        input = new Channel(Float.TYPE, 2);
        output = new Channel(Float.TYPE, 2);
        scale = (float)Math.sqrt(2);
    }
    public TwoPointDCT() {
        super();
    }
}

class decimate extends Filter
{
    private int N;
    private float[] coef;
    public void work() {
        float[] in_arr = new float[N];
        for (int i  = 0; (i < N); i++) {in_arr[i] = input.popFloat();}
        for (int i  = 0; (i < (N / 2)); i++) {output.pushFloat((in_arr[i] + in_arr[(i + (N / 2))]));}
        float temp ;
        for (int i  = 0; (i < (N / 2)); i++) {{
            temp = (in_arr[i] - in_arr[(i + (N / 2))]);
            output.pushFloat((temp * coef[i]));
        }}
    }
    public void init(final int N)     {
        this.N = N;
        this.coef = new float[(N / 2)];
        input = new Channel(Float.TYPE, N);
        output = new Channel(Float.TYPE, N);
        for (int i  = 0; (i < (N / 2)); i++) {coef[i] = (1 / (2 * ((float)Math.sqrt((2 / N)) * (float)Math.cos((((2 * i) + 1) * (3.141592653589793 / (2 * N)))))));}
    }
    public decimate(final int N) {
        super(N);
    }
}

class bitrev extends SplitJoin
{
    private int N;
    public void init(final int N) {
        this.N = N;
            setSplitter(ROUND_ROBIN((N / 2)));
            add(new DamnIdentity());
            add(new reversal((N / 2)));
            setJoiner(ROUND_ROBIN((N / 2)));
        }
    public bitrev(final int N) {
        super(N);
    }
}

class DamnIdentity extends Filter
{
    public void work() {
        output.pushFloat(input.popFloat());
    }
    public void init() {
        input = new Channel(Float.TYPE, 1);
        output = new Channel(Float.TYPE, 1);
    }
    public DamnIdentity() {
        super();
    }
}

class reversal extends Filter
{
    private int N;
    public void work() {
        for (int i  = (N - 1); (i >= 0); i--) {output.pushFloat(input.peekFloat(i));}
        for (int i  = 0; (i < N); i++) {input.popFloat();}
    }
    public void init(final int N) {
        this.N = N;
        input = new Channel(Float.TYPE, N, N);
        output = new Channel(Float.TYPE, N);
    }
    public reversal(final int N) {
        super(N);
    }
}

