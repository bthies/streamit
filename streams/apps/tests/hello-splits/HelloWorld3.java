/*

  Hello World Program #3:

  1) Generates "Hello World!" string one character at a
  time.

  2) Encodes the message by having a feedback loop, XOR'ing each character
     with the one that is offset by 3

  4) prints out some encoded message

 */

import streamit.library.*;

public class HelloWorld3 extends StreamIt
{

    // presumably some main function invokes the stream
    public static void main(String args[])
    {
        new HelloWorld3().run(args);
    }

    // this is the defining part of the stream
    public void init ()
    {
        add(new CharGenerator());
        add(new SplitJoin()
            {
                public void init()
                {
                    setSplitter(ROUND_ROBIN ());
                    add(new XORLoop());
                    //add(new ChannelConnectFilter (Character.TYPE));
                    //add(new ChannelConnectFilter (Character.TYPE));
                    setJoiner(ROUND_ROBIN());
                }
            });
        add(new CharPrinter());
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
    public int initPath (int index)
    {
        return ' ';
    }

    public void init() {
        setDelay(3);
        setJoiner (ROUND_ROBIN ());
        setBody (new XORFilter());
	setLoop (new Identity(Character.TYPE));
        setSplitter (DUPLICATE ());
    }
}

// outputs xor of successive items in stream
class XORFilter extends Filter
{
    public void work()
    {
        char c1 = input.popChar();
        char c2 = input.popChar();
        output.pushChar((char)((int)c1 ^ (int)c2));
    }
    public void init ()
    {
        input = new Channel(Character.TYPE, 2);
        output = new Channel(Character.TYPE, 1);
    }
}

// buffers input until it gets a null-terminated string, then prints it
// to the screen
/*
class BufferedCharPrinter extends Filter
{
    // string it's queueing up
    private StringBuffer sb;

    public void init()
    {
        input = new Channel(Character.TYPE, 1);
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
*/
