package streamit;

public class ChannelConnectFilter extends Filter
{
    Class type;
    public ChannelConnectFilter (Class ioType)
    {
        super ();
        type = ioType;
    }

    public ChannelConnectFilter () { super (); }

    public void init ()
    {
        if (type != null)
        {
            input = new Channel (type, 1);
            output = new Channel (type, 1);
        }
    }

    public void work()
    {
        passOneData (input, output);
    }

    void useChannels (Channel in, Channel out)
    {
        ASSERT (input == null && output == null);
        ASSERT (in != null && out != null);
        ASSERT (in != out);
        ASSERT (out.getType ().getName ().equals (in.getType ().getName ()));

        input = in;
        output = out;

        input.setSink (this);
        output.setSource (this);
    }
}
