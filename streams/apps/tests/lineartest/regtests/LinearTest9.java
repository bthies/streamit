/*
parsing:
done parsing. outputing:
 <# pipeline <# -> void void #> SimpleStreamItProgram <# { <# add FloatSource ( #> <# add LinearPipe ( #> <# add FloatSink ( #> #> #> <# filter <# -> void float #> FloatSource { <# x float #> <# init <# { <# = x 0 #> #> #> <# work <# push 1 #> <# { <# push x #> <# = x <# + x 1 #> #> #> #> #> <# filter <# -> float void #> FloatSink { <# work <# pop 1 #> <# { <# $call println <# ( pop #> #> #> #> #> <# filter <# -> float float #> FloatIdentity { <# work <# pop 1 #> <# push 1 #> <# { <# push pop #> #> #> #> <# filter <# -> float float #> LinearFilter { <# work <# pop 3 #> <# push 1 #> <# { <# t1 float <# = pop #> #> <# t2 float <# = pop #> #> <# t3 float <# = pop #> #> <# push <# + t1 <# + t2 t3 #> #> #> #> #> #> <# pipeline <# -> float float #> LinearPipe <# { <# add LinearFilter ( #> <# add FloatIdentity ( #> #> #> null
done outputing. walking:
*/
import streamit.*;
import streamit.io.*;

class Complex extends Structure {
  public double real, imag;
}

class SimpleStreamItProgram extends StreamIt
{
    static public void main (String [] args)
    {
        new SimpleStreamItProgram().run (args);
    }
    public void init() {
            add(new FloatSource());
            add(new LinearPipe());
            add(new FloatSink());
        }
}

class FloatSource extends Filter
{
    private float x;
    public void work() {
        output.pushFloat(x);
        x = (x + 1);
    }
    public void init()     {
        output = new Channel(Float.TYPE, 1);
        x = 0;
    }
}

class FloatSink extends Filter
{
    public void work() {
        System.out.println(input.popFloat());
    }
    public void init() {
        input = new Channel(Float.TYPE, 1);
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
}

class LinearFilter extends Filter
{
    public void work() {
        float t1 = input.popFloat();
        float t2 = input.popFloat();
        float t3 = input.popFloat();
        output.pushFloat((t1 + (t2 + t3)));
    }
    public void init() {
        input = new Channel(Float.TYPE, 3);
        output = new Channel(Float.TYPE, 1);
    }
}

class LinearPipe extends Pipeline
{
    public void init() {
            add(new LinearFilter());
            add(new FloatIdentity());
        }
}

