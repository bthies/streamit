/*

  Hello World Program #5:
  
  runs in parallel with one of the previous HelloWorld programs, then
  switches to a different one to run in parallel with once it has
  finished.

 */

import streamit.library.*;

public class HelloWorld5 extends SplitJoin {

    public HelloWorld5(int n) {
	super(n);
    }

    // presumably some main function invokes the stream
    public static void main(String args[]) {
	new HelloWorld5(1).run(args);
    }

    // if init with no arguments, then init with random 
    public void init() 
    {
	init(1 + (int)(Math.random()*4));
    }

    // this is the defining part of the stream
    public void init(int selection)
    {
	// setup the one to run in parallel
	add(new Stream() {
		public void init() {
		    add(new CharGenerator());
		    add(new ResetingCharPrinter(this));
		}
	    });
	// setup the other one to run.  this acts like the old SwitchSelect.
	switch(selection) {
	case 1: add(new HelloWorld1()); break;
	case 2: add(new HelloWorld2()); break;
	case 3: add(new HelloWorld3()); break;
	case 4: add(new HelloWorld4()); break;
	}
    }
}

class ResetingCharPrinter extends Filter {
    
    private Channel input = new Channel(new char[0]);
    private Channel output = null;
    
    // stream to reset when we're done
    private Stream str;

    public ResetingCharPrinter(Stream str) {
	super(str);
    }

    public void init(Stream str) {
	this.str = str;
    }
    
    public void work() {
	char c;
	System.out.print(c = input.popChar());
	if (c=='\0') {
	    sendMessage(str.reset());
	}
    }
    
}
