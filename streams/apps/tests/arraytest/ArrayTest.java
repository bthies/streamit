import streamit.library.*;

/**
 * this should print 1.0, 2.0, 3.0 ...
 */
class ArrayTest extends StreamIt
{
    static public void main (String [] t)
    {
        ArrayTest test = new ArrayTest ();
        test.run (t);
    }

    public void init ()
    {
	add (new Filter() {
		float val ;
		public void init() {
		    output = new Channel(new float[0][0].getClass(), 1);
		    this.val = 1.0f;
		}

		public void work ()
		{
		    float[][] myArray = new float[1][1];
		    myArray[0][0] = val;
		    val = val + 1;
		    output.push2DFloat (myArray);
		}
	    });
	add (new Filter() {
		public void init() {
		    input = new Channel(new float[0][0].getClass(), 1);
		}

		public void work ()
		{
		    System.out.println (input.pop2DFloat ()[0][0]);
		}
	    });
    }
}

	
