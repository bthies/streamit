/*

  Hello World Program #3:

  1) Generates the ".....Hello World!.....\0" string one character at a
  time.  This time the string has to be null-terminated so that the
  printers recognize the end.

  2) Encodes the message by having a feedback loop, XOR'ing each character
     with the one that is offset by 3

  3) queue's up and prints out the encoded message

  3) Decodes the message by the same process

  4) queue's up and prints out the final message

 */

import streamit.*;

public class HelloWorld3 extends StreamIt
{

    // presumably some main function invokes the stream
    public static void main(String args[])
    {
        new HelloWorld3().run();
    }

    // this is the defining part of the stream
    public void init ()
    {
        add(new CharGenerator(".....Hello World!.....\0"));
        add(new SplitJoin()
            {
                public void init()
                {
                    setSplitter(ROUND_ROBIN ());
                    add(new ChannelConnectFilter (Character.TYPE));
                    add(new XORLoop());
                    //add(new ChannelConnectFilter (Character.TYPE));
                    //add(new ChannelConnectFilter (Character.TYPE));
                    setJoiner(ROUND_ROBIN());
                }
            });
        add(new BufferedCharPrinter());
    }

    /* here is an alternative way to write the above code:

    public void init() {
        add(new CharGenerator(".....Hello World!.....\0"));
        add(new XORLoop());
        add(new SplitJoin() {
                public void init() {
                    splitter(Splitter.DUPLICATE_SPLITTER);
                    add(new BufferedCharPrinter());
                    add(new Stream() {
                            public void init() {
                                add(new XORLoop());
                                add(new BufferedCharPrinter());
                            }
                        });
                }
            });
    }

     */

}

class XORLoop extends FeedbackLoop
{
    public void initPath (int index, Channel path)
    {
        path.pushChar ((char)0);
    }

    public void init() {
        setDelay(3);
        setJoiner (ROUND_ROBIN ());
        setBody (new XORFilter());
        setSplitter (DUPLICATE ());
    }
}

// outputs xor of successive items in stream
class XORFilter extends Filter
{
    Channel input = new Channel(Character.TYPE, 2);
    Channel output = new Channel(Character.TYPE, 1);
    public void initIO ()
    {
        streamInput = input;
        streamOutput = output;
    }

    public void work()
    {
        char c1 = input.popChar();
        char c2 = input.popChar();
        output.pushChar((char)((int)c1 ^ (int)c2));
    }
}

// buffers input until it gets a null-terminated string, then prints it
// to the screen
class BufferedCharPrinter extends Filter
{
    Channel input = new Channel(Character.TYPE, 1);
    public void initIO ()
    {
        streamInput = input;
    }

    // string it's queueing up
    private StringBuffer sb;

    public void init()
    {
        sb = new StringBuffer();
    }

    public void work()
    {
        char c = input.popChar();
        // flush the buffer if we hit null-terminated string
        if (c=='\0')
        {
            System.out.println("@" + sb + "@");
            sb = new StringBuffer();
        } else {
            sb.append(c);
        }
    }
}
