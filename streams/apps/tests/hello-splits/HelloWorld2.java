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
        new HelloWorld2().Run();
    }

    // this is the defining part of the stream
    public void Init()
    {
        Add(new CharGenerator("Hello World2!"));
        Add(new SplitJoin()
        {
            public void Init()
            {
                SetSplitter(ROUND_ROBIN ());
                Add(new Identity (Character.TYPE));
                Add(new Identity (Character.TYPE));
                SetJoiner (ROUND_ROBIN ());
            }
        });
        Add (new SplitJoin ()
        {
            public void Init ()
            {
                SetSplitter (WEIGHTED_ROUND_ROBIN (2, 1, 4, 2));
                Add (new Filter ()
                {
                    public void InitIO ()
                    {
                        input = new Channel(Character.TYPE);
                        output = new Channel (Character.TYPE);
                    }

                    public void InitCount ()
                    {
                        inCount = 1;
                        outCount = 2;
                    }

                    public void Work()
                    {
                           input.PopChar ();
                           output.PushChar (input.PopChar ());
                    }
                });
                Add (new Filter ()
                {
                    public void InitIO ()
                    {
                        input = new Channel(Character.TYPE);
                        output = new Channel (Character.TYPE);
                    }

                    public void InitCount ()
                    {
                        inCount = 1;
                        outCount = 8;
                    }

                    public void Work()
                    {
                        char c = input.PopChar ();
                        output.PushChar (c);
                        output.PushChar (c);
                        output.PushChar (c);
                        output.PushChar (c);
                        output.PushChar (c);
                        output.PushChar (c);
                        output.PushChar (c);
                        output.PushChar (c);
                    }
                });
                Add (new Identity (Character.TYPE));
                Add(new CharPrinter());
                SetJoiner (WEIGHTED_ROUND_ROBIN (1, 2, 1, 0));
            }
        });
        Add (new CharPrinter ());
    }
}
