package streamit;

// creates a split/join
public class SplitJoin extends Stream
{
    public Channel input = null;
    public Channel output = null;

    Splitter splitter = null;
    Joiner joiner = null;

    public SplitJoin() 
    {
        super();
    }

    public SplitJoin(int n) 
    {
        super(n);
    }

    // specify the splitter
    public void UseSplitter(Splitter s) 
    {
        ASSERT (splitter == null && s != null);
        splitter = s;
    }

    // specify the joiner
    public void UseJoiner(Joiner j) 
    {
        ASSERT (joiner == null && j != null);
        joiner = j;
    }

    // add a stream to the parallel section between the splitter and the joiner
    public void Add(Stream s)
    {
        Channel newInput = s.GetIOField ("input");
        Channel newOutput = s.GetIOField ("output");
        
        if (input == null)
        {
            input = newInput;
        } else 
        if (newInput != null) 
        {
            // check that the input types agree
            ASSERT (newInput.GetType ().getName ().equals (input.GetType ().getName ()));
        }
        
        if (output == null)
        {
            output = newOutput;
        } else 
        if (newOutput != null) 
        {
            // check that the output types agree
            ASSERT (newOutput.GetType ().getName ().equals (output.GetType ().getName ()));
        }
        
        if (splitter != null)
        {
            splitter.Add (s);
        } else {
            ASSERT (newInput == null);
        }
        
        if (joiner != null) 
        {
            joiner.Add (s);
        } else {
            ASSERT (newOutput == null);
        }
    }
}
