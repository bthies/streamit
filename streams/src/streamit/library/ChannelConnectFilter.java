package streamit;

public class ChannelConnectFilter extends Filter 
{
    Class type;
    public ChannelConnectFilter (Class ioType)
    {
        super ();
        type = ioType;
        InitIO ();
    }
    
    public ChannelConnectFilter () { super (); }
    
    public void InitIO ()
    {
        if (type != null)
        {
            input = new Channel (type);
            output = new Channel (type);
        }
    }
    
    public void Init () { }

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
