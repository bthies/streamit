/*
 * Flybit.java: an interesting piece of the Butterfly example
 * (to demonstrate split/joins)
 * $Id: Flybit.java,v 1.2 2001-10-10 19:02:26 dmaze Exp $
 */

import streamit.*;

public class Flybit extends Pipeline
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
                                output.pushInt(input.popInt()-input.popInt());
                            }
                        });
                    add(new Filter()
                        {
                            Channel input = new Channel(Integer.TYPE, 2);
                            Channel output = new Channel(Integer.TYPE, 2);
                            public void work()
                            {
                                output.pushInt(input.popInt()+input.popInt());
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

        
