package streamit;

public class RoundRobinJoiner extends Joiner {
    public RoundRobinJoiner() 
    {
    }
    
    public void Work ()
    {
        while (inputCount == srcsWeight [inputIndex])
        {
            inputCount = 0;
            inputIndex = (inputIndex + 1) % srcs.size ();
        }
        
        PassOneData (input [inputIndex], output);
        inputCount++;
    }
}
