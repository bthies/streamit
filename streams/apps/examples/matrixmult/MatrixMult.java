import streamit.library.StreamIt;
import streamit.library.Pipeline;
import streamit.library.SplitJoin;
import streamit.library.Filter;
import streamit.library.Channel;
import streamit.library.Identity;
import streamit.library.io.FileWriter;

public class MatrixMult extends StreamIt
{
    public static void main (String [] args)
    {
        new MatrixMult ().run (args);
    }
    
    public void init ()
    {
        add (new FloatSource (10));
        //add (new MatrixMultiply (3, 4, 5, 3));
	add (new MatrixMultiply (10, 10, 10, 10));
        //add (new FileWriter ("matmul.out", Float.TYPE));
	add(new FloatPrinter());
	//add (new Sink());
    }
}

class MatrixMultiply extends Pipeline
{
    MatrixMultiply (int a, int b, int c, int d)
    {
        super (a, b, c, d);
    }
    
    public void init (final int x0, final int y0, final int x1, final int y1)
    {
        //ASSERT (x0 == y1);
        
        // rearrange and duplicate the matrices as necessary:
        add (new RearrangeDuplicateBoth (x0, y0, x1, y1));
	add (new MultiplyAccumulateParallel (x0, x0));
	//add (new MultiplyAccumulate (x0));
    }
}

class RearrangeDuplicateBoth extends SplitJoin
{
    RearrangeDuplicateBoth (int a, int b, int c, int d)
    {
        super (a, b, c, d);
    }
    
    public void init (int x0, int y0, int x1, int y1)
    {
        setSplitter (WEIGHTED_ROUND_ROBIN (x0 * y0, x1 * y1));
        // the first matrix just needs to get duplicated 
        add (new DuplicateRows (x1, x0));
        
        // the second matrix needs to be transposed first
        // and then duplicated:
        add (new RearrangeDuplicate (x0, y0, x1, y1));
        setJoiner (ROUND_ROBIN ());
    }
}

class RearrangeDuplicate extends Pipeline
{
    RearrangeDuplicate (int x0, int y0, int x1, int y1)
    {
        super (x0, y0, x1, y1);
    }
    
    public void init (int x0, int y0, int x1, int y1)
    {
        add (new Transpose (x1, y1));
        add (new DuplicateRows (y0, x1*y1));
    }
}


class Transpose extends SplitJoin
{
    Transpose (int a, int b)
    {
        super (a, b);
    }
    
    public void init (int x, int y)
    {
	int i;
        setSplitter (ROUND_ROBIN ());
        for (i=0; i<x; i++) {
            add (new Identity (Float.TYPE));
        }
        setJoiner (ROUND_ROBIN (y));
    }
    
}

class MultiplyAccumulateParallel extends SplitJoin
{
    MultiplyAccumulateParallel(int x, int n)
    {
        super(x, n);
    }
    
    public void init(int x, int n)
    {
        int i;
        setSplitter(ROUND_ROBIN(x * 2));
        for (i = 0; i < n; i++)
            add (new MultiplyAccumulate(x));
        setJoiner(ROUND_ROBIN(1));
    }
}

class MultiplyAccumulate extends Filter
{
    MultiplyAccumulate (int x)
    {
        super (x);
    }
    
    int rowLength;
    
    public void init (int length)
    {
        rowLength = length;
        
        input = new Channel (Float.TYPE, length * 2);
        output = new Channel (Float.TYPE, 1);
    }
    
    public void work ()
    {
        float result = 0;
	int x;
        for (x= 0; x < rowLength; x++)
        {
            result += (input.peekFloat (0) * input.peekFloat (1));
	    input.popFloat(); input.popFloat();
        }
        output.pushFloat (result);
    }
}

class DuplicateRows extends Pipeline
{
    DuplicateRows (int x, int y) { super (x, y); }
    public void init (int x, int y) { add (new DuplicateRowsInternal (x, y)); }
}

class DuplicateRowsInternal extends SplitJoin
{
    DuplicateRowsInternal (int x, int y)
    {
        super (x, y);
    }
    
    int rowLength;
    int numTimes;
    
    public void init (final int times, int length)
    {
	int i;
        setSplitter (DUPLICATE ());
        for (i=0; i<times; i++) add (new Identity (Float.TYPE));
        setJoiner (ROUND_ROBIN (length));
    }
}

class FloatSource extends Filter
{
    FloatSource (float maxNum)
    {
        super (maxNum);
    }
    
    float num;
    float maxNum;
    
    public void init (float maxNum2)
    {
        output = new Channel (Float.TYPE, 1);
        this.maxNum = maxNum2;
        this.num = 0;
    }
    
    public void work ()
    {
        output.pushFloat (num);
        num++;
        if (num == maxNum) num = 0;
    }
}

class FloatPrinter extends Filter
{
    FloatPrinter ()
    {
	super();
    }
    
    public void init () {
        input = new Channel (Float.TYPE, 1);
    }
    
    public void work ()
    {
        System.out.println(input.popFloat());
    }
}

class Sink extends Filter {
    int x;
    public void init() {
	input = new Channel(Float.TYPE, 1);
	x = 0;
    }
    public void work() {
	input.popFloat();
	x++;
	if (x==100) {
	    System.out.println("done..");
	    x = 0;
	}
    }
}
