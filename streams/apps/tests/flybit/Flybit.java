/*
 * Flybit.java: an interesting piece of the Butterfly example
 * (to demonstrate split/joins)
 * $Id: Flybit.java,v 1.1 2001-10-10 15:17:49 dmaze Exp $
 */

import streamit.*;

class Flybit extends Pipeline
{
    static public void main(String[] t)
    {
        Flybit test = new Flybit();
        test.run();
    }
    
    public void init()
    {
        add(new Filter()
            {
                int x;
                Channel output = new Channel(Integer.TYPE, 1);
                public void init()
                {
                    this.x = 0;
                }
                public void work()
                {
                    output.pushInt(x++);
                }
            });
        add(new SplitJoin()
            {
                Channel input = new Channel(Integer.TYPE, 2);
                Channel output = new Channel(Integer.TYPE, 2);
                public void init() 
                {
                    setSplitter(DUPLICATE());
                    add(new Filter()
                        {
                            Channel input = new Channel(Integer.TYPE, 2);
                            Channel output = new Channel(Integer.TYPE, 2);
                            public void work()
                            {
                                int val1 = ((Integer)input.pop()).intValue();
                                int val2 = ((Integer)input.pop()).intValue();
                                output.push(new Integer(val1 - val2));
                            }
                        });
                    add(new Filter()
                        {
                            Channel input = new Channel(Integer.TYPE, 2);
                            Channel output = new Channel(Integer.TYPE, 2);
                            public void work()
                            {
                                int val1 = ((Integer)input.pop()).intValue();
                                int val2 = ((Integer)input.pop()).intValue();
                                output.push(new Integer(val1 + val2));
                            }
                        });
                    setJoiner(WEIGHTED_ROUND_ROBIN(2, 2));
                }
            });
        add (new Filter()
            {
                Channel input = new Channel(Integer.TYPE, 1);
                public void initIO ()
                {
                    streamInput = input;
                } 
                public void work ()
                {
                    System.out.println (input.popInt ());
                }
            });
    }
}

        
