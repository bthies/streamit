package streamit;

// creates a split/join
public class SplitJoin extends Stream {

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
    }

    // specify the joiner
    public void UseJoiner(Joiner s) 
    {
    }

    // add a stream to the parallel section between the splitter and the joiner
    public void Add(Stream s) 
    {
    }

}
