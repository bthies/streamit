/*

  Hello World Program #2:

  1) Generates the "Hello World!" string one character at a time

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
    	Add(new CharGenerator("Hello World!\n"));
    	Add(new SplitJoin() 
    	{
    		public void Init() 
    		{
    		    UseSplitter(Splitter.ROUND_ROBIN_SPLITTER);
    		    Add(new CharPrinter());
    		    Add(new CharPrinter());
    		    Add(new CharPrinter());
    		}
        });
    }
}
