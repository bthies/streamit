/*

  Hello World Program #2:

  1) Generates the "Hello World!" string one character at a time

  2) Splits the characters through a split/join, guaranteeing only a
  partial ordering on the order in which the characters are printed.

  3) Prints out chars in split join

 */

import streamit.library.*;

public class HelloWorld2 extends StreamIt
{

    // presumably some main function invokes the stream
    public static void main(String args[])
    {
        new HelloWorld2().run(args);
    }

    // this is the defining part of the stream
    public void init()
    {
        this.add(new CharGenerator());
        this.add(new SplitJoin()
        {
            public void init()
            {
                setSplitter(ROUND_ROBIN ());
                this.add(new Identity (Character.TYPE));
                this.add(new Identity (Character.TYPE));
                setJoiner (ROUND_ROBIN ());
            }
        });
        this.add (new Pipeline ()
        {
            public void init ()
            {
                this.add (new Pipeline ()
                {
                    public void init ()
                    {
                        this.add (new SplitJoin ()
                        {
                            public void init ()
                            {
                                setSplitter (WEIGHTED_ROUND_ROBIN (2, 1, 4, 2));
                                this.add (new Filter ()
                                {
                                    public void init ()
                                    {
                                        input = new Channel(Character.TYPE, 1);
                                        output = new Channel (Character.TYPE, 2);
                                    }
                                    public void work()
                                    {
                                       input.popChar ();
                                       output.pushChar (input.popChar ());
                                    }
                                });
                                this.add (new Filter ()
                                {
                                    public void init ()
                                    {
                                        input = new Channel(Character.TYPE, 1);
                                        output = new Channel (Character.TYPE, 8);
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
                                this.add (new Identity (Character.TYPE));
                                this.add(new CharPrinter());
                                setJoiner (WEIGHTED_ROUND_ROBIN (1, 2, 1, 0));
                            }
                        });
                    }
                });
            }
        });
        this.add (new CharPrinter ());
    }
}
