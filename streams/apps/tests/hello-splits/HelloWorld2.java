/*

  Hello World Program #2:

  1) Generates the "Hello World!" string one character at a time

  2) Splits the characters through a split/join, guaranteeing only a
  partial ordering on the order in which the characters are printed.

  3) Prints out chars in split join

 */

import streamit.*;

public class HelloWorld2 extends Stream {

    // presumably some main function invokes the stream
    public static void main(String args[]) {
	new HelloWorld2().run();
    }

    // this is the defining part of the stream
    public void init() {
	add(new CharGenerator("Hello World!"));
	add(new SplitJoin() {
		public void init() {
		    splitter(Splitter.ROUND_ROBIN_SPLITTER);
		    add(new CharPrinter());
		    add(new CharPrinter());
		    add(new CharPrinter());
		}
	    });
    }

}
