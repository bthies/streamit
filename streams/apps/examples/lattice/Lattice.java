import streamit.library.*;
import streamit.library.io.*;

class LatDel extends SplitJoin {// this generates the delays in the lattice structure
  
    

public void init() {
    setSplitter(DUPLICATE());
     add(new Identity(Float.TYPE));
     add(new Delay(1));
    setJoiner(ROUND_ROBIN());
     }
}

class LatFilt extends Filter {// this is the intermediate stage of the lattice filter
  float k_par;

    public LatFilt(float k_par) {super (k_par);}

  public void init(float k_par) {
      input = new Channel(Float.TYPE, 2, 2);
      output = new Channel(Float.TYPE, 2);
   this.k_par=k_par;
   }
  
 public void work(){
  float e_i=0;
  float e_bar_i=0;
  e_i=input.peekFloat(0)-k_par*input.peekFloat(1);
  e_bar_i=input.peekFloat(1)-k_par*input.peekFloat(0);
  output.pushFloat(e_i);
  output.pushFloat(e_bar_i);
  input.popFloat();
  input.popFloat();
  }
}

class ZeroStage extends SplitJoin { //this stage is the first stage of a lattice filter
  public void init() {
   setSplitter(DUPLICATE());
   add(new Identity(Float.TYPE));
   add(new Identity(Float.TYPE));
   setJoiner(ROUND_ROBIN());
   }
}

class CompStage extends Pipeline {// this class combines the delaying phase and coefficients

    public CompStage(float k_par) {super (k_par);}

    public void init(float k_par) {
	add(new LatDel());
        add(new LatFilt(k_par));
    }
 }




class LastStage extends Filter {   // this class is the last stage of a lattice filter
public void init(){ 
    input = new Channel(Float.TYPE, 2);
   }

public void work(){
    System.out.println (input.popFloat());    
    input.popFloat();
 }
}

class Counter extends Filter {
    float i;
public void init(){ 
    output = new Channel(Float.TYPE, 1);
   i = 1;
   }

public void work(){
    output.pushFloat(i);
    i = 0;
 }
}

class Lattice extends StreamIt {
    static public void main (String[] t)
    {
        Lattice test = new Lattice();
        test.run(t);
    }

public void init() {
   add(new Counter());
   add(new ZeroStage());
   for (int i=2; i<10; i++)
   add(new CompStage(i));
   add(new LastStage());
  }
}

class Delay extends FeedbackLoop {

    public Delay(int N) {
	super(N);
    }

    public void init(int N) {
	setSplitter(ROUND_ROBIN());
	setDelay(N);
	setBody(new Filter() {
                public void init() {
		    input = new Channel(Float.TYPE, 2);
		    output = new Channel(Float.TYPE, 2);
                }
                public void work() {
		    this.output.pushFloat(this.input.peekFloat(1));
		    this.output.pushFloat(this.input.peekFloat(0));
		    this.input.popFloat();
		    this.input.popFloat();
                }
	    });
	setLoop(new Identity(Float.TYPE));
	setJoiner(ROUND_ROBIN());
    }

    public float initPathFloat(int index) {
	return 0.0f;
    }
}

   









