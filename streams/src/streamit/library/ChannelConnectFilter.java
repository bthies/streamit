package streamit;

public class ChannelConnectFilter extends Filter
{
    Class type;
    public ChannelConnectFilter (Class ioType)
    {
        super ();
        type = ioType;
        initIO ();
    }

    public ChannelConnectFilter () { super (); }

    public void initIO ()
    {
        if (type != null)
        {
            streamInput = new Channel (type);
            streamOutput = new Channel (type);
        }
    }

    public void init () { }

    public void work()
    {
        passOneData (streamInput, streamOutput);
    }

    void useChannels (Channel in, Channel out)
    {
        ASSERT (streamInput == null && streamOutput == null);
        ASSERT (in != null && out != null);
        ASSERT (in != out);
        ASSERT (out.getType ().getName ().equals (in.getType ().getName ()));

        streamInput = in;
        streamOutput = out;

        streamInput.setSink (this);
        streamOutput.setSource (this);
    }
}
