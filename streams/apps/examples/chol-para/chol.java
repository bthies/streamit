/*
parsing:
done parsing. outputing:
 <# pipeline <# -> void void #> chol ( <# { <# N int <# = 100 #> #> <# add source <# ( N #> #> <# add rchol <# ( N #> #> <# add recon <# ( N #> #> <# add sink <# ( N #> #> #> #> <# pipeline <# -> float float #> rchol <# ( <# N int #> #> <# { <# add divises <# ( N #> #> <# add updates <# ( N #> #> <# if <# > N 1 #> <# add break1 <# ( N #> #> #> #> #> <# splitjoin <# -> float float #> break1 <# ( <# N int #> #> <# { <# split roundrobin <# ( N <# / <# * N <# - N 1 #> #> 2 #> #> #> <# add FloatIdentity ( #> <# add rchol <# ( <# - N 1 #> #> #> <# join roundrobin <# ( N <# / <# * N <# - N 1 #> #> 2 #> #> #> #> #> <# filter <# -> float float #> FloatIdentity ( { <# work <# pop 1 #> <# push 1 #> <# { <# push pop #> #> #> #> <# filter <# -> float float #> divises <# ( <# N int #> #> { <# init { #> <# work <# push <# / <# * N <# + N 1 #> #> 2 #> #> <# pop <# / <# * N <# + N 1 #> #> 2 #> #> <# { <# temp1 float #> <# = temp1 pop #> <# = temp1 <# $call sqrt <# ( temp1 #> #> #> <# push temp1 #> <# for <# i int <# = 1 #> #> <# < i N #> <# ++ i #> <# push <# / pop temp1 #> #> #> <# for <# i int <# = 0 #> #> <# < i <# / <# * N <# - N 1 #> #> 2 #> #> <# ++ i #> <# push pop #> #> #> #> #> <# filter <# -> float float #> updates <# ( <# N int #> #> { <# init { #> <# work <# pop <# / <# * N <# + N 1 #> #> 2 #> #> <# push <# / <# * N <# + N 1 #> #> 2 #> #> <# { <# temp <# [ float N #> #> <# for <# i int <# = 0 #> #> <# < i N #> <# ++ i #> <# { <# = <# $array temp <# [ i #> #> pop #> <# push <# $array temp <# [ i #> #> #> #> #> <# for <# i int <# = 1 #> #> <# < i N #> <# ++ i #> <# for <# j int <# = i #> #> <# < j N #> <# ++ j #> <# push <# - pop <# * <# $array temp <# [ i #> #> <# $array temp <# [ j #> #> #> #> #> #> #> #> #> #> <# filter <# -> void float #> source <# ( <# N int #> #> { <# init { #> <# work <# pop 0 #> <# push <# / <# * N <# + N 1 #> #> 2 #> #> <# { <# for <# i int <# = 0 #> #> <# < i N #> <# ++ i #> <# { <# push <# * <# + i 1 #> <# * <# + i 1 #> 100 #> #> #> <# for <# j int <# = <# + i 1 #> #> #> <# < j N #> <# ++ j #> <# push <# * i j #> #> #> #> #> #> #> #> <# filter <# -> float float #> recon <# ( <# N int #> #> { <# init { #> <# work <# pop <# / <# * N <# + N 1 #> #> 2 #> #> <# push <# / <# * N <# + N 1 #> #> 2 #> #> <# { <# L <# [ float N N #> #> <# sum float <# = 0 #> #> <# for <# i int <# = 0 #> #> <# < i N #> <# ++ i #> <# for <# j int <# = 0 #> #> <# < j N #> <# ++ j #> <# = <# $array L <# [ i j #> #> 0 #> #> #> <# for <# i int <# = 0 #> #> <# < i N #> <# ++ i #> <# for <# j int <# = i #> #> <# < j N #> <# ++ j #> <# = <# $array L <# [ j i #> #> pop #> #> #> <# for <# i int <# = 0 #> #> <# < i N #> <# ++ i #> <# for <# j int <# = i #> #> <# < j N #> <# ++ j #> <# { <# = sum 0 #> <# for <# k int <# = 0 #> #> <# < k N #> <# ++ k #> <# += sum <# * <# $array L <# [ i k #> #> <# $array L <# [ j k #> #> #> #> #> <# push sum #> #> #> #> #> #> #> <# filter <# -> float void #> sink <# ( <# N int #> #> { <# init { #> <# work <# pop <# / <# * N <# + N 1 #> #> 2 #> #> <# push 0 #> <# { <# for <# i int <# = 0 #> #> <# < i N #> <# ++ i #> <# for <# j int <# = i #> #> <# < j N #> <# ++ j #> <# { <# $call println <# ( pop #> #> #> #> #> #> #> #> null
done outputing. walking:
*/
import streamit.*;
import streamit.io.*;

class Complex extends Structure {
  public double real, imag;
}

class chol extends StreamIt
{
    static public void main (String [] args)
    {
        new chol().run (args);
    }
    public void init() {
            int N  = 100;
            add(new source(N));
            add(new rchol(N));
            add(new recon(N));
            add(new sink(N));
        }
    public chol() {
        super();
    }
}

class rchol extends Pipeline
{
    private int N;
    public void init(final int N) {
        this.N = N;
            add(new divises(N));
            add(new updates(N));
            if ((N > 1)) {add(new break1(N));}
        }
    public rchol(final int N) {
        super(N);
    }
}

class break1 extends SplitJoin
{
    private int N;
    public void init(final int N) {
        this.N = N;
            setSplitter(WEIGHTED_ROUND_ROBIN(N, ((N * (N - 1)) / 2)));
            add(new FloatIdentity());
            add(new rchol((N - 1)));
            setJoiner(WEIGHTED_ROUND_ROBIN(N, ((N * (N - 1)) / 2)));
        }
    public break1(final int N) {
        super(N);
    }
}

class FloatIdentity extends Filter
{
    public void work() {
        output.pushFloat(input.popFloat());
    }
    public void init() {
        input = new Channel(Float.TYPE, 1);
        output = new Channel(Float.TYPE, 1);
    }
    public FloatIdentity() {
        super();
    }
}

class divises extends Filter
{
    private int N;
    public void work() {
        float temp1 ;
        temp1 = input.popFloat();
        temp1 = (float)Math.sqrt(temp1);
        output.pushFloat(temp1);
        for (int i  = 1; (i < N); ++i) {output.pushFloat((input.popFloat() / temp1));}
        for (int i  = 0; (i < ((N * (N - 1)) / 2)); ++i) {output.pushFloat(input.popFloat());}
    }
    public void init(final int N)     {
        this.N = N;
        input = new Channel(Float.TYPE, ((N * (N + 1)) / 2));
        output = new Channel(Float.TYPE, ((N * (N + 1)) / 2));
    }
    public divises(final int N) {
        super(N);
    }
}

class updates extends Filter
{
    private int N;
    public void work() {
        float[] temp = new float[N];
        for (int i  = 0; (i < N); ++i) {{
            temp[i] = input.popFloat();
            output.pushFloat(temp[i]);
        }}
        for (int i  = 1; (i < N); ++i) {for (int j  = i; (j < N); ++j) {output.pushFloat((input.popFloat() - (temp[i] * temp[j])));}}
    }
    public void init(final int N)     {
        this.N = N;
        input = new Channel(Float.TYPE, ((N * (N + 1)) / 2));
        output = new Channel(Float.TYPE, ((N * (N + 1)) / 2));
    }
    public updates(final int N) {
        super(N);
    }
}

class source extends Filter
{
    private int N;
    public void work() {
        for (int i  = 0; (i < N); ++i) {{
            output.pushFloat(((i + 1) * ((i + 1) * 100)));
            for (int j  = (i + 1); (j < N); ++j) {output.pushFloat((i * j));}
        }}
    }
    public void init(final int N)     {
        this.N = N;
        output = new Channel(Float.TYPE, ((N * (N + 1)) / 2));
    }
    public source(final int N) {
        super(N);
    }
}

class recon extends Filter
{
    private int N;
    public void work() {
        float[][] L = new float[N][N];
        float sum  = 0;
        for (int i  = 0; (i < N); ++i) {for (int j  = 0; (j < N); ++j) {L[i][j] = 0;}}
        for (int i  = 0; (i < N); ++i) {for (int j  = i; (j < N); ++j) {L[j][i] = input.popFloat();}}
        for (int i  = 0; (i < N); ++i) {for (int j  = i; (j < N); ++j) {{
            sum = 0;
            for (int k  = 0; (k < N); ++k) {sum += (L[i][k] * L[j][k]);}
            output.pushFloat(sum);
        }}}
    }
    public void init(final int N)     {
        this.N = N;
        input = new Channel(Float.TYPE, ((N * (N + 1)) / 2));
        output = new Channel(Float.TYPE, ((N * (N + 1)) / 2));
    }
    public recon(final int N) {
        super(N);
    }
}

class sink extends Filter
{
    private int N;
    public void work() {
        for (int i  = 0; (i < N); ++i) {for (int j  = i; (j < N); ++j) {{
            System.out.println(input.popFloat());
        }}}
    }
    public void init(final int N)     {
        this.N = N;
        input = new Channel(Float.TYPE, ((N * (N + 1)) / 2));
    }
    public sink(final int N) {
        super(N);
    }
}

