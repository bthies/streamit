/*

  Hello World Program #1 - Generates the "Hello World!" string one
  character at a time and prints it to the screen.
 */

import streamit.library.*;

public class HelloWorld1 extends StreamIt
{
    // presumably some main function invokes the stream
    public static void main(String args[])
    {
           new HelloWorld1().run(args);
    }

    // this is the defining part of the stream
    public void init()
    {
        add (new Pipeline ()
        {
            public void init ()
            {
                add(new CharGenerator());
            }
        });
        add (new Pipeline ()
        {
            public void init ()
            {
                add (new Filter ()
                {
                    public void init ()
                    {
                        input = new Channel (Character.TYPE, 1, 2);
                        output = new Channel (Character.TYPE, 1);
                    }
                    public void work ()
                    {
                        char c = input.popChar ();
                        output.pushChar (c);
                    }
                });
            }
        });
        add (new Pipeline ()
        {
            public void init ()
            {
                add(new CharPrinter());
            }
        });
    }
}




