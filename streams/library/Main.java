import streamit.*;
 
class AddOne extends Filter
{
    int x;
    Channel output = new Channel (Integer.TYPE, 1);    /* push */
    public void init() {
	x = 100000;
    }
    public void work(){
	x = x + 1;
	output.pushInt (x);
    }
}

class Display extends Filter 
{
    Channel input = new Channel (Integer.TYPE, 1);     /* pop [peek] */
    
    public void work ()
    {
	System.out.println (input.popInt ());
    }
}

public class Main extends Pipeline
{
    public void init() 
    {
	add(new AddOne());
	add (new Display());
    }
}
