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

public class HelloWorld3 extends Stream
{

    // presumably some main function invokes the stream
    public static void main(String args[])
    {
        new HelloWorld3().Run();
    }

    // this is the defining part of the stream
    public void Init ()
    {
        Add(new CharGenerator(".....Hello World!.....\0"));
        //Add(new XORLoop());
        Add(new SplitJoin() 
            {
                public void Init() 
                {
                    SetSplitter(WEIGHTED_ROUND_ROBIN (2, 2));
                    Add(new ChannelConnectFilter (new char [1]));
                    //Add(new XORLoop());
                    //Add(new ChannelConnectFilter (new char [1]));
                    Add(new ChannelConnectFilter (new char [1]));
                    SetJoiner(ROUND_ROBIN());
                }
            });
        Add(new BufferedCharPrinter());
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
/*
class XORLoop extends FeedbackLoop 
{
    public void Init() {
        SetDelay(3);
        Header(Joiner.ROUND_ROBIN_JOINER ());
        Add(new XORFilter());
    }
}
*/
// outputs xor of successive items in stream
class XORFilter extends Filter
{
    public void InitIO ()
    {
        input = new Channel(new char[0]);
        output = new Channel(new char[0]);
    }

    public void Work()
    {
        char c1 = input.PopChar();
        char c2 = input.PopChar();
        output.PushChar((char)((int)c1 ^ (int)c2));
    }
}

// buffers input until it gets a null-terminated string, then prints it
// to the screen
class BufferedCharPrinter extends Filter 
{
    public void InitIO ()
    {
        input = new Channel(new char[0]);
    }

    // string it's queueing up
    private StringBuffer sb;

    public void Init() 
    {
        sb = new StringBuffer();
    }

    public void Work()
    {
        char c = input.PopChar();
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
