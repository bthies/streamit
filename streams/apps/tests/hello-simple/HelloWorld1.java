/*

  Hello World Program #1 - Generates the "Hello World!" string one
  character at a time and prints it to the screen.

 */

import streamit.*;

public class HelloWorld1 extends Stream {

    // presumably some main function invokes the stream
    public static void main(String args[]) {
	new HelloWorld1().run();
    }

    // this is the defining part of the stream
    public void init() {
	add(new CharGenerator("Hello World!"));
	add(new CharPrinter());
    }

}

class CharGenerator extends Filter {

    private Channel input = null;
    private Channel output = new Channel(new char[0]);

    public CharGenerator(String str) {
	super(str);
    }

    // the position in outputting
    private int i;
    // the string to output
    private String message;

    // <message> is string to output, one char at a time
    public void init(String message) {
	// init counter
	i = 0;
	// init message
	this.message = message;
    }

    public void work() {
	output.pushChar(message.charAt(0));
	i++;
    }

}

class CharPrinter extends Filter {

    private Channel input = new Channel(new char[0]);
    private Channel output = null;

    public void work() {
	System.out.print(input.popChar());
    }

}


