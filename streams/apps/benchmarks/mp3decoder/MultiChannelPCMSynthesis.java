import streamit.SplitJoin;

public class MultiChannelPCMSynthesis extends SplitJoin
{
    final int granuleInputSize = (/* data size */ 18 + /* for type */ 1) * 32 + /* for type */ 1;
    
    public MultiChannelPCMSynthesis(int n)
    {
        super(n);
    }

    public void init(int nChannels)
    {
        // depending on how many channels, initialize the splitter appropriately
        if (nChannels == 1)
        {
            setSplitter(WEIGHTED_ROUND_ROBIN(granuleInputSize));
        } else if (nChannels == 2)
        {
            setSplitter(WEIGHTED_ROUND_ROBIN(granuleInputSize, granuleInputSize));
        } else
            ERROR("you must have 1 or 2 channels in your MP3!");

        int ch;
        for (ch = 0; ch < nChannels; ch++)
        {
            add(new FilterBank());
        }

        setJoiner(ROUND_ROBIN());
    }
}