/*

  Hello World Program #1 - Generates the "Hello World!" string one
  character at a time and prints it to the screen.

 */

import streamit.*;

public class HelloWorld1 extends Stream {

    // presumably some main function invokes the stream
    public static void main(String args[]) 
    {
	   new HelloWorld1().Run();
    }

    // this is the defining part of the stream
    public void Init() 
    {
    	Add(new CharGenerator("Hello World!"));
    	Add(new CharPrinter());
    }

}



