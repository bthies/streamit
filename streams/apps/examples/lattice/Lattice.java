import streamit.*;
import streamit.io.*;

class Delay extends Filter{
    public Delay(int d) {  super(d); }

  public void init(int delay){
    setInput(Float.TYPE); setOutput(Float.TYPE);
    setPush(1); setPop(1);
    for (int i=0; i<delay;i++)
       output.pushFloat(0);
    }
 public void work() {output.push(input.pop());}
}

class LatDel extends SplitJoin {// this generates the delays in the lattice structure
  public void init() {
    setSplitter(DUPLICATE());
     add(new Delay(0));
     add(new Delay(1));
    setJoiner(ROUND_ROBIN());
     }
}

class LatFilt extends Filter {// this is the intermediate stage of the lattice filter
  float k_par;

    public LatFilt(float k_par) {super (k_par);}

  public void init(float k_par) {
   setInput(Float.TYPE); setOutput(Float.TYPE);
   setPush(2);setPop(2);setPeek(2);
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
   add(new Delay(0));
   add(new Delay(0));
   setJoiner(ROUND_ROBIN());
   }
}

class LastStage extends Filter {   // this class is the last stage of a lattice filter
public void init(){ 
   setInput(Float.TYPE); setOutput(Float.TYPE);
   setPush(1);setPop(2);setPeek(0);
   }

public void work(){
    output.pushFloat(input.popFloat());    
    input.popFloat();
 }
}

class Counter extends Filter {   // this class is the last stage of a lattice filter
    float i;
public void init(){ 
   setInput(Float.TYPE); setOutput(Float.TYPE);
   setPush(1);setPop(0);
   i = 0;
   }

public void work(){
    output.pushFloat(i);
    i = i+1;
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
   add(new LatFilt(2));
   add(new LastStage());
  }
}


   





