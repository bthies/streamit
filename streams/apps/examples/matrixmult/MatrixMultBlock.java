import streamit.StreamIt;
import streamit.Pipeline;
import streamit.SplitJoin;
import streamit.Filter;
import streamit.Channel;
import streamit.Identity;

/* $Id: MatrixMultBlock.java,v 1.1 2002-07-28 18:25:54 karczma Exp $ */

public class MatrixMultBlock extends StreamIt
{
    public static void main(String[] args)
    {
        new MatrixMultBlock().run(args);
    }

    public void init()
    {
        add(new BlockFloatSource(4));
        //add(new BlockMatrixFloatPrinter(4, 4));
        add(new MatrixBlockMultiply(4, 4, 4, 4, 2));
        // sink
        add (new Filter() {
            public void init ()
            {
                input = new Channel (Float.TYPE, 1);
            }
            public void work ()
            {
                input.popFloat ();
            }
        });
    }
}

class MatrixBlockMultiply extends Pipeline
{
    MatrixBlockMultiply(int a, int b, int c, int d, int e)
    {
        super(a, b, c, d, e);
    }

    public void init(
        final int x0,
        final int y0,
        final int x1,
        final int y1,
        final int blockSize)
    {
        ASSERT(x0 == y1);
        ASSERT(x0 % blockSize == 0);
        ASSERT(y0 % blockSize == 0);
        ASSERT(x1 % blockSize == 0);
        ASSERT(y1 % blockSize == 0);

        // rearrange and duplicate the matrices as necessary:
        add(new SplitJoin()
        {
            public void init()
            {
                setSplitter(WEIGHTED_ROUND_ROBIN(x0 * y0, x1 * y1));
                add(new Pipeline()
                {
                    public void init()
                    {
                        add(new BlockSplit(x0, y0, blockSize));
                        add(
                            new Duplicate(
                                x0 * y0 / (blockSize),
                                blockSize));
                    }
                });
                add(new Pipeline()
                {
                    public void init()
                    {
                        add(new Transpose(x1, y1));
                        add(new BlockSplit(x1, y1, blockSize));
                        add(new Duplicate(x1 * y1, blockSize));
                    }
                });
                setJoiner(
                    WEIGHTED_ROUND_ROBIN(
                        x0 * y0 / (blockSize * blockSize),
                        x1 * y1 / (blockSize * blockSize)));
            }
        });
        add(
            new BlockMultiply(
                x0 / blockSize,
                y0 / blockSize,
                y1 / blockSize,
                x0 / blockSize));
        add(new BlockAdd(x1 / blockSize, y0 / blockSize, blockSize));
        add(new BlockCombine(x1, y0, blockSize));
        add(new BlockMatrixFloatPrinter(4, 4));
    }
}

class BlockSplit extends SplitJoin
{
    BlockSplit(int a, int b, int c)
    {
        super(a, b, c);
    }
    public void init(int x0, int y0, int blockSize)
    {
        setSplitter(ROUND_ROBIN(x0 / blockSize));
        int i;
        for (i = 0; i < blockSize; i++)
        {
            add(new Identity(Float.TYPE));
        }
        setJoiner(ROUND_ROBIN(x0 * y0 / (blockSize * blockSize)));
    }
}

class BlockCombine extends Pipeline
{
    BlockCombine(int a, int b, int c)
    {
        super(a, b, c);
    }
    public void init(int x0, int y0, int blockSize)
    {
        add (new BlockSplit(x0, y0, blockSize));
    }
}

class BlockAdd extends Filter
{
    BlockAdd(int a, int b, int c)
    {
        super(a, b, c);
    }
    int x, y, times;
    float[][] result;
    public void init(int _x, int _y, int _times)
    {
        x = _x;
        y = _y;
        times = _times;
        result = new float[x][y];

        input = new Channel(Float.TYPE, x * y * times);
        output = new Channel(Float.TYPE, x * y);
    }

    public void work()
    {
        int a, b;
        for (b = 0; b < y; b++)
        {
            for (a = 0; a < x; a++)
            {
                result[a][b] = input.popFloat();
            }
        }

        int c;
        for (c = 1; c < times; c++)
        {
            for (b = 0; b < y; b++)
            {
                for (a = 0; a < x; a++)
                {
                    result[a][b] += input.popFloat();
                }
            }
        }

        for (b = 0; b < y; b++)
        {
            for (a = 0; a < x; a++)
            {
                output.pushFloat(result[a][b]);
            }
        }
    }
}

class Transpose extends SplitJoin
{
    Transpose(int a, int b)
    {
        super(a, b);
    }

    public void init(int x, int y)
    {
        int i;
        setSplitter(ROUND_ROBIN());
        for (i = 0; i < x; i++)
        {
            add(new Identity(Float.TYPE));
        }
        setJoiner(ROUND_ROBIN(y));
    }

}

class BlockMultiply extends Filter
{
    BlockMultiply(int a, int b, int c, int d)
    {
        super(a, b, c, d);
    }

    int x0, y0, x1, y1;

    public void init(int _x0, int _y0, int _x1, int _y1)
    {
        x0 = _x0;
        y0 = _y0;
        x1 = _x1;
        y1 = _y1;

        input = new Channel(Float.TYPE, x0 * y0 + x1 * y1);
        output = new Channel(Float.TYPE, y0 * x1);
    }

    public void work()
    {
        int block2Start = x0 * y0;
        int x, y, z;
        for (y = 0; y < y0; y++)
        {
            for (x = 0; x < x1; x++)
            {
                float sum = 0;
                for (z = 0; z < x0; z++)
                {
                    sum
                        += (input.peekFloat(z + y * y0)
                            * input.peekFloat(z + x * y0 + block2Start));
                }
                output.pushFloat(sum);
            }
        }

        for (x = 0; x < x0 * y0 + x1 * y1; x++)
            input.popFloat();
    }
}

class Duplicate extends SplitJoin
{
    Duplicate(int x, int y)
    {
        super(x, y);
    }
    public void init(int amnt, int ways)
    {
        setSplitter(DUPLICATE());
        int a;
        for (a = 0; a < ways; a++)
        {
            add(new Identity(Float.TYPE));
        }
        setJoiner(ROUND_ROBIN(amnt));
    }
}

class BlockFloatSource extends Filter
{
    BlockFloatSource(float maxNum)
    {
        super(maxNum);
    }

    float num;
    float maxNum;

    public void init(float maxNum2)
    {
        output = new Channel(Float.TYPE, 1);
        this.maxNum = maxNum2;
        this.num = 0;
    }

    public void work()
    {
        output.pushFloat(num);
        num++;
        if (num == maxNum)
            num = 0;
    }
}

class BlockMatrixFloatPrinter extends Filter
{
    BlockMatrixFloatPrinter(int x, int y)
    {
        super(x, y);
    }
    int x, y;
    public void init(int x2, int y2)
    {
        input = new Channel(Float.TYPE, x2 * y2);
        output = new Channel (Float.TYPE, x2 * y2);
        this.x = x2;
        this.y = y2;
    }
    public void work()
    {
        int a, b;
        for (b = 0; b < y; b++)
        {
            for (a = 0; a < x; a++)
            {
                float data = input.popFloat ();
                output.pushFloat (data);
                System.out.print(data + " ");
            }
            System.out.println();
        }
    }
}
