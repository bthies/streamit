/*

  Hello World Program #4:

  1) Generates the ".....Hello World!.....\0......" string one character
  at a time.  This time the string has to be null-terminated so that the
  printers recognize the end.

  4) queue's up and prints out the final message.  when message is
  written to screen, the first node receieves a message to start over.

 */

import streamit.library.*;

public class HelloWorld4 extends Stream {

    // presumably some main function invokes the stream
    public static void main(String args[]) {
	new HelloWorld4().run(args);
    }

    // this is the defining part of the stream
    public void init() {
	MessagingCharGenerator generator;
	add(generator = new MessagingCharGenerator(".....Hello World!.....\0"));
	add(new MessagingBufferedCharPrinter(generator));
    }
}

final class MessagingCharGenerator extends Filter {

    private Channel input = null;
    private Channel output = new Channel(Character.TYPE);

    public MessagingCharGenerator(String str) {
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

    // to reset counter upon a message
    MessageStub StartOver() {
	i = 0;
	return MESSAGE_STUB;
    }
}

// buffers input until it gets a null-terminated string, then prints it
// to the screen
final class MessagingBufferedCharPrinter extends Filter {

    private Channel input = new Channel(Character.TYPE);
    private Channel output = null;

    // string it's queueing up
    private StringBuffer sb;
    // the generator that's making the characters
    private MessagingCharGenerator generator;

    public MessagingBufferedCharPrinter(Stream str) {
	super(str);
    }

    public void init(MessagingCharGenerator generator) {
	sb = new StringBuffer();
	this.generator = generator;
    }

    public void work() {
	char c = input.popChar();
	sb.append(c);
	// if we hit null-terminated string, print to screen, flush the
	// buffer, and send message to generator
	if (c=='\0') {
	    // print to screen
	    System.out.println(sb);
	    // flush buffer
	    sb = new StringBuffer();
	    // send message to generator
	    sendMessage(generator.startOver());
	}
    }
}


