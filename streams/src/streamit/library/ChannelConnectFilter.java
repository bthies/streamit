package streamit;

public class ChannelConnectFilter extends Filter 
{
    public ChannelConnectFilter (Object ioType)
    {
        super ();
        input = new Channel (ioType);
        output = new Channel (ioType);
    }
    
    public ChannelConnectFilter () { super (); }
    
    public void InitIO ()
    {
    }

    public void Work()
    {
        PassOneData (input, output);
    }
    
    void UseChannels (Channel in, Channel out)
    {
        ASSERT (input == null && output == null);
        ASSERT (in != null && out != null);
        ASSERT (in != out);
        ASSERT (out.GetType ().getName ().equals (in.GetType ().getName ()));
        
        input = in;
        output = out;
        
        input.SetSink (this);
        output.SetSource (this);
    }
}
