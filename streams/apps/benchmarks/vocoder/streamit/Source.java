import streamit.*;
import streamit.io.*;

class RandSource extends Filter
{
  final int length = 6;
  int i = 0;
  int x = 1;
  
    public void work() {
        output.pushInt(x);
	if (i < length)
	  x = 2 * x + 1;
	else
	  x = (x - 1) / 2;

        i++;
	if (i == (length * 2)) {
	  x = 1; i = 0;
	}
	//        System.out.println(("Random: "+i));
    }
    public void init() {
        output = new Channel(Integer.TYPE, 1);
    }
    public RandSource() {
        super();
    }
}

class StepSource extends Filter
{
  final int length = 6;
  int i = 0;
  int x = 1;
  int up = 1;
  
    public void work() {
        output.pushInt(x);
	if (i == length) {
	  i = 0;
	  if (up == 1) {
	    x++;
	  } else {
	    x--;
	  }
	  if (x == length) {
	    up = -1; i = 0;
	  }
	  if (x == 0) {
	    up = 1;
	  }
	} else {
	  i++;
	}

	//        System.out.println(("Random: "+i));
    }
    public void init() {
        output = new Channel(Integer.TYPE, 1);
    }
    public StepSource() {
        super();
    }
}

