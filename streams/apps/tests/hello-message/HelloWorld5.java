/*

  Hello World Program #5:

  runs in parallel with one of the previous HelloWorld programs, then
  switches to a different one to run in parallel with once it has
  finished.

 */

import streamit.*;

public class HelloWorld5 extends SplitJoin {

    public HelloWorld5(int n) {
	super(n);
    }

    // presumably some main function invokes the stream
    public static void main(String args[]) {
	new HelloWorld5(1).run();
    }

    // if init with no arguments, then init with random 
    public void Init() 
    {
	Init(1 + (int)(Math.random()*4));
    }

    // this is the defining part of the stream
    public void Init(int selection)
    {
	// setup the one to run in parallel
	Add(new Stream() {
		public void init() {
		    Add(new CharGenerator("Hello World!.....\0"));
		    Add(new ResetingCharPrinter(this));
		}
	    });
	// setup the other one to run.  this acts like the old SwitchSelect.
	switch(selection) {
	case 1: Add(new HelloWorld1()); break;
	case 2: Add(new HelloWorld2()); break;
	case 3: Add(new HelloWorld3()); break;
	case 4: Add(new HelloWorld4()); break;
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

    public void Init(Stream str) {
	this.str = str;
    }
    
    public void Work() {
	char c;
	System.out.print(c = input.popChar());
	if (c=='\0') {
	    sendMessage(str.reset());
	}
    }
    
}
