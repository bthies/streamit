import streamit.*;
import streamit.io.*;

class AdapTest extends StreamIt {
    static public void main (String[] t)
    
    {
	AdapTest test=new AdapTest();
	test.run(t);
    }
    public void init() {
	float[][] C=
      {
       	{1,2,3},
  	{0,0,0},
  	{0,0,0}
	};
   	add(new Source());
	add(new GenA(2,3,3,3,C));
	add(new Sink());
    }
}
    
    

class Source extends Filter {
    float d[][]={
	{1,0,0},
	{0,1,0}
    };
    public void init(){
	setOutput(Float.TYPE);
	setPush(6);
	    }
    public void work(){
	for(int i=0;i<3;i++)
	    for(int j=0;j<2;j++)
		output.pushFloat(d[j][i]);
    }
}

class Sink extends Filter{
    float A[][];
    public void init(){
	setInput(Float.TYPE);
	setPop(3*3*(3*3+1));
	A = new float[10][9];
     }
    public void work() {
	for (int i=0; i< 3*3+1;i++)
	    for (int j=0; j<3*3;j++)
		A[i][j]=input.popFloat();
	for (int i=0; i< 9;i++)
	    {for (int j=0;j<10;j++)
		System.out.println(A[j][i]);
	    System.out.println("col finished");
	    System.out.println(i);
	    System.out.println("don col");
	    }
    }
}
		    
		

















