import streamit.library.StreamIt;
import streamit.library.Pipeline;
import streamit.library.SplitJoin;
import streamit.library.Filter;
import streamit.library.Channel;
import streamit.library.Identity;

/* $Id: MatrixMultBlock.java,v 1.7 2003-09-29 09:07:14 thies Exp $ */

public class MatrixMultBlock extends StreamIt
{
    public static void main(String[] args)
    {
        new MatrixMultBlock().run(args);
    }

    public void init()
    {
        int x0 = 12;
        int y0 = 12;
        int x1 = 9;
        int y1 = 12;
        int blockDiv = 3;

        add(new BlockFloatSource(4));
        add(new MatrixBlockMultiply(x0, y0, x1, y1, blockDiv));
        add(new BlockMatrixFloatPrinter(x1, y0));
        // sink
        add(new Filter()
        {
            public void init()
            {
                input = new Channel(Float.TYPE, 1);
            }
            public void work()
            {
                input.popFloat();
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
        final int blockDiv)
    {
        //ASSERT(x0 == y1);
        //ASSERT(x0 % blockDiv == 0);
        //ASSERT(y0 % blockDiv == 0);
        //ASSERT(x1 % blockDiv == 0);
        //ASSERT(y1 % blockDiv == 0);

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
                        add(new BlockSplit(x0, y0, blockDiv));
                        add(new Duplicate(x0 * y0 / (blockDiv), blockDiv));
                    }
                });
                add(new Pipeline()
                {
                    public void init()
                    {
                        add(new Transpose(x1, y1));
                        add(new BlockSplit(y1, x1, blockDiv));
                        add(new Duplicate(x1 * y1, blockDiv));
                    }
                });
                setJoiner(
                    WEIGHTED_ROUND_ROBIN(
                        x0 * y0 / (blockDiv * blockDiv),
                        x1 * y1 / (blockDiv * blockDiv)));
            }
        });

        add(new SplitJoin()
        {
            public void init()
            {
                setSplitter(ROUND_ROBIN((x0 * y0 + x1 * y1)
                                                / blockDiv));
                int y;
                for (y = 0; y < blockDiv; y++)
                {
                    add(new Pipeline()
                    {
                        public void init()
                        {
                            add(new SplitJoin()
                            {
                                public void init()
                                {
                                    setSplitter(
                                        ROUND_ROBIN(
                                            (x0 * y0 + x1 * y1)
                                                / (blockDiv * blockDiv)));
                                    int x;
                                    for (x = 0; x < blockDiv; x++)
                                    {
                                        add(
                                            new BlockMultiply(
                                                x0 / blockDiv,
                                                y0 / blockDiv,
                                                x1 / blockDiv,
                                                y1 / blockDiv));
                                    }
                                    setJoiner(
                                        ROUND_ROBIN(
                                            x1 * y0 / (blockDiv * blockDiv)));
                                }
                            });
                            add(
                                new BlockAdd(
                                    x1 / blockDiv,
                                    y0 / blockDiv,
                                    blockDiv));
                            //add(new BlockMatrixFloatPrinter(x1/blockDiv, y0/blockDiv));
                        }
                    });
                }
                setJoiner(ROUND_ROBIN(x1 * y0 / (blockDiv * blockDiv)));
            }
        });
        add(new BlockCombine(x1, y0, blockDiv));
    }
}

class BlockSplit extends SplitJoin
{
    BlockSplit(int a, int b, int c)
    {
        super(a, b, c);
    }
    public void init(int x0, int y0, int blockDiv)
    {
        setSplitter(ROUND_ROBIN(x0 / blockDiv));
        int i;
        for (i = 0; i < blockDiv; i++)
        {
            add(new Identity(Float.TYPE));
        }
        setJoiner(ROUND_ROBIN(x0 * y0 / (blockDiv * blockDiv)));
    }
}

class BlockCombine extends Pipeline
{
    BlockCombine(int a, int b, int c)
    {
        super(a, b, c);
    }
    public void init(int x0, int y0, int blockDiv)
    {
        add(new BlockSplit(x0*y0/(blockDiv*blockDiv), y0, y0/blockDiv));
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
        result = new float[_x][_y];

        input = new Channel(Float.TYPE, _x * _y * _times);
        output = new Channel(Float.TYPE, _x * _y);
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
        
        //ASSERT (_x0 == _y1);

        input = new Channel(Float.TYPE, _x0 * _y0 + _x1 * _y1);
        output = new Channel(Float.TYPE, _y0 * _x1);
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
                    int leftPos = z + y * x0;
                    int rightPos = z + x * y1 + block2Start;
                    
                    float left = input.peekFloat(leftPos);
                    float right = input.peekFloat(rightPos);
                    sum
                        += (left * right);
                    //System.out.println (left + "(" + leftPos + ") * " + right + "(" + rightPos + ")");
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
        output = new Channel(Float.TYPE, x2 * y2);
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
                float item = input.popFloat();
                output.pushFloat(item);
                System.out.println(item);
            }
            //System.out.println();
        }
    }
}
