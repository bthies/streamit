import streamit.library.*;

public class CharGenerator extends Filter
{
    public CharGenerator()
    {
        super();
    }

    // the position in outputting
    private int i;
    // the string to output
    private char[] message;

    // <message> is string to output, one char at a time
    public void init()
    {
	message = new char[13];
	message[0]  = 'H';
	message[1]  = 'e';
	message[2]  = 'l';
	message[3]  = 'l';
	message[4]  = 'o';
	message[5]  = ' ';
	message[6]  = 'W';
	message[7]  = 'o';
	message[8]  = 'r';
	message[9]  = 'l';
	message[10] = 'd';
	message[11] = '!';
	message[12] = ' ';
        output = new Channel (Character.TYPE, 1);

        // init counter
        i = 0;
    }

    public void work()
    {
        output.pushChar(message[i]);
        i++;
        if (i == 13)
        {
            i = 0;
        }
    }

}
