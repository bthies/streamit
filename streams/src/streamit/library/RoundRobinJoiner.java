package streamit;

public class RoundRobinJoiner extends Joiner {
    public RoundRobinJoiner() 
    {
    }
    
    public void Work ()
    {
        PassOneData (input [inputIndex], output);
        inputIndex = (inputIndex + 1) % srcs.size ();
    }
}
