import streamit.*;

public class CharGenerator extends Filter {

    public Channel output = new Channel(new char[1]);

    public CharGenerator(String str) 
    {
        super(str);
    }

    // the position in outputting
    private int i;
    // the string to output
    private String message;

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
