/*

  Hello World Program #2:

  1) Generates the "Hello World2!" string one character at a time

  2) Splits the characters through a split/join, guaranteeing only a
  partial ordering on the order in which the characters are printed.

  3) Prints out chars in split join

 */

import streamit.*;

public class HelloWorld2 extends Stream
{

    // presumably some main function invokes the stream
    public static void main(String args[])
    {
        new HelloWorld2().run();
    }

    // this is the defining part of the stream
    public void init()
    {
        add(new CharGenerator("Hello World2!"));
        add(new SplitJoin()
        {
            public void init()
            {
                setSplitter(ROUND_ROBIN ());
                add(new Identity (Character.TYPE));
                add(new Identity (Character.TYPE));
                setJoiner (ROUND_ROBIN ());
            }
        });
        add (new SplitJoin ()
        {
            public void init ()
            {
                setSplitter (WEIGHTED_ROUND_ROBIN (2, 1, 4, 2));
                add (new Filter ()
                {
                    Channel input = new Channel(Character.TYPE, 1);
                    Channel output = new Channel (Character.TYPE, 2);

                    public void initIO ()
                    {
                        streamInput = input;
                        streamOutput = output;
                    }

                    public void work()
                    {
                       input.popChar ();
                       output.pushChar (input.popChar ());
                    }
                });
                add (new Filter ()
                {
                    Channel input = new Channel(Character.TYPE, 1);
                    Channel output = new Channel (Character.TYPE, 8);

                    public void initIO ()
                    {
                        streamInput = input;
                        streamOutput = output;
                    }

                    public void work()
                    {
                        char c = input.popChar ();
                        output.pushChar (c);
                        output.pushChar (c);
                        output.pushChar (c);
                        output.pushChar (c);
                        output.pushChar (c);
                        output.pushChar (c);
                        output.pushChar (c);
                        output.pushChar (c);
                    }
                });
                add (new Identity (Character.TYPE));
                add(new CharPrinter());
                setJoiner (WEIGHTED_ROUND_ROBIN (1, 2, 1, 0));
            }
        });
        add (new CharPrinter ());
    }
}
