/*

  Hello World Program #1 - Generates the "Hello World!" string one
  character at a time and prints it to the screen.
 */

import streamit.*;

public class HelloWorld1 extends Stream
{
    public void initIO () { }
    // presumably some main function invokes the stream
    public static void main(String args[])
    {
           new HelloWorld1().run();
    }

    // this is the defining part of the stream
    public void Init()
    {
        add(new CharGenerator("Hello World!"));
        add(new CharPrinter());
    }
}




