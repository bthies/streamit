import streamit.*;

public class CharGenerator extends Filter
{
    public CharGenerator(String str)
    {
        super(str);
    }

    // the position in outputting
    private int i;
    // the string to output
    private String message;

    public void InitIO ()
    {
        output = new Channel (Character.TYPE);
    }

    // <message> is string to output, one char at a time
    public void Init(String message)
    {
        // init counter
        i = 0;
        // init message
        this.message = message;
    }

    public void Work()
    {
        output.PushChar(message.charAt(i));
        i++;
        if (message.length () == i)
        {
            i = 0;
            output.PushChar ('\n');
        }
    }

}
