//===========================================================================
//
//   FILE: FileTest.java:
//   
//   Author: Michael Gordon
//   Date: Tue Nov  6 17:58:35 2001
//
//   Function:  Simple i/o test
//
//===========================================================================



import streamit.library.*;
import streamit.library.io.*;

public class FileTest extends StreamIt {
    
    public static void main (String[] args) {
	FileTest test = new FileTest();
	test.run(args);
    }

    public void init() {
	add (new Filter ()
        {
            float x;
            public void init() {
                output = new Channel (Float.TYPE, 1);    /* push */
                this.x = 0;
            }
            public void work ()
            {
		x = x + 2;
                output.pushFloat (x);
            }
        });
	add (new FileWriter("float.test", Float.TYPE));
    }
}
