import streamit.library.SplitJoin;
import streamit.library.Pipeline;

public class MultiChannelPCMSynthesis extends Pipeline
{
    public MultiChannelPCMSynthesis (int n) { super (n); }

    public void init (int nChannels)
    {
        add (new MultiChannelPCMSynthesisInternal (nChannels));
    }
}

class MultiChannelPCMSynthesisInternal extends SplitJoin
{
    public MultiChannelPCMSynthesisInternal(int n)
    {
        super(n);
    }

    public void init(int nChannels)
    {
        int ch;

        int granuleInputSize = (/* data size */ 18 + /* for type */ 1) * 32 + /* for type */ 1;
        // depending on how many channels, initialize the splitter appropriately
        // ASSERT (nChannels == 1 || nChannels == 2);

        setSplitter(ROUND_ROBIN((/* data size */ 18 + /* for type */ 1) * 32 + /* for type */ 1));

        for (ch = 0; ch < nChannels; ch++)
        {
            add(new FilterBank());
        }

        setJoiner(ROUND_ROBIN());
    }
}
