
import streamit.library.*;
class ArrayTest3 extends StreamIt
{
    static public void main (String [] t)
    {
        ArrayTest3 test = new ArrayTest3 ();
        test.run (t);
    }

    public void init ()
    {
	add (new Filter() {
		float val ;
		float myArray[][];
		public void init() {
		    output = new Channel(new float[4][4].getClass(), 1);
		    this.val = 1.0f;
		}

		public void work ()
		{
		    int i, j;

		    myArray = new float[4][4];
	
		    for (i = 0; i < 4; i++)
			for (j = 0; j < 4; j++) {
			    myArray[i][j] = val;
			    val = val + 1;  
			}
		    output.push2DFloat (myArray);
		}
	    });
	add (new SplitJoin() {
		public void init() {
		    setSplitter(ROUND_ROBIN());
		    add (new Filter() {
			    public void init() {
				output = new Channel(new float[3][3].getClass(), 1);
				input = new Channel(new float[4][4].getClass(), 1);
			    }
			    public void work() {
				int i, j;
				
				float popArray[][];
				float myArray[][] = new float[3][3];
				popArray = new float[4][4];
				
				popArray = input.pop2DFloat();
				
				popArray[2][2] = 3;
				
				for (i = 0; i < 3; i++)
				    for (j = 0; j < 3; j++) {
					myArray[i][j] = popArray[i][j];
				    }
				output.push2DFloat(myArray);
			    }});
		   add (new Filter() {
			    public void init() {
				output = new Channel(new float[3][3].getClass(), 1);
				input = new Channel(new float[4][4].getClass(), 1);
			    }
			    public void work() {
				int i, j;
				
				float popArray[][];
				float myArray[][] = new float[3][3];
				popArray = new float[4][4];
				
				popArray = input.pop2DFloat();
				
				popArray[2][2] = 3;
				
				for (i = 0; i < 3; i++)
				    for (j = 0; j < 3; j++) {
					myArray[i][j] = popArray[i][j];
				    }
				output.push2DFloat(myArray);
			    }});
		   setJoiner(ROUND_ROBIN());
		}});
	add (new Filter() {
		public void init() {
		    output = new Channel(new float[3][3].getClass(), 1);
		    input = new Channel(new float[3][3].getClass(), 2);	
		}
		public void work() {
		    float temp[][] = new float[3][3];
		    int i, j;
		   
		    for (i = 0; i < 3; i++)
			for (j = 0; j < 3; j++)
			    temp[i][j] = input.peek2DFloat(0)[i][j] + 
				input.peek2DFloat(1)[i][j];
		    input.pop2DFloat();
		    input.pop2DFloat();
		    output.push2DFloat(temp);
		}}); 
	add (new Filter() {
		public void init() {
		    input = new Channel(new float[3][3].getClass(), 1);
		}

		public void work ()
		{
		    System.out.println (input.pop2DFloat ()[0][0]);
		}
	    });
    }
}
