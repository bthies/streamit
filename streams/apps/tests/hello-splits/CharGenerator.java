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

    Channel output = new Channel (Character.TYPE, 1);

    public void initIO ()
    {
        streamOutput = output;
    }

    // <message> is string to output, one char at a time
    public void init(String message)
    {
        // init counter
        i = 0;
        // init message
        this.message = message;
    }

    public void work()
    {
        streamOutput.pushChar(message.charAt(i));
        i++;
        if (message.length () == i)
        {
            i = 0;
            streamOutput.pushChar ('\n');
        }
    }

}
