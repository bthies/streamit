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

    // <message> is string to output, one char at a time
    public void init(String message)
    {
        output = new Channel (Character.TYPE, 1);

        // init counter
        i = 0;
        // init message
        this.message = message;
    }

    public void work()
    {
        output.pushChar(message.charAt(i));
        i++;
        if (message.length () == i)
        {
            i = 0;
            output.pushChar ('\n');
        }
    }

}
