import streamit.Pipeline;
import streamit.SplitJoin;
import streamit.Filter;
import streamit.Channel;

class FloatIdentity extends Filter
{
    public FloatIdentity()
    {
        super();
    }
    public void init()
    {
        input = new Channel(Float.TYPE, 1);
        output = new Channel(Float.TYPE, 1);
    }
    public void work()
    {
        output.pushFloat(input.popFloat());
    }
}

public class FilterBank extends Pipeline
{
    public void init()
    {
        add(new Antialias());
        add(new IMDCT());

        // overlapping:
        add(new Filter()
        {
            public void init()
            {
                input = new Channel(Float.TYPE, 36, 36 + 1152);
                output = new Channel(Float.TYPE, 18);
            }

            public void work()
            {
                int i;
                for (i = 0; i < 18; i++)
                {
                    float future1 = input.peekFloat(1152);
                    float future2 = input.peekFloat(18);
                    output.pushFloat(future1 + future2);
                    input.popFloat();
                }
                for (i = 0; i < 18; i++)
                {
                    input.popFloat();
                }
            }
        });

        // frequency inversion
        add(new SplitJoin()
        {
            public void init()
            {
                setSplitter(ROUND_ROBIN());
                int x;

                for (x = 0; x < 18; x++)
                {
                    if (x % 2 == 0)
                    {
                        add(new FloatIdentity());
                    } else
                    {
                        add(new SplitJoin()
                        {
                            public void init()
                            {
                                setSplitter(ROUND_ROBIN());
                                add(new FloatIdentity());
                                add(new Filter()
                                {
                                    public void init()
                                    {
                                        input = new Channel(Float.TYPE, 1);
                                        output = new Channel(Float.TYPE, 1);
                                    }
                                    public void work()
                                    {
                                        output.pushFloat(-input.popFloat());
                                    }
                                });
                                setJoiner(ROUND_ROBIN());
                            }
                        });
                    }
                }

                // join the 18 producers and put them in entire 32-sample
                // subband pieces
                setJoiner(
                    WEIGHTED_ROUND_ROBIN(
                        32,
                        32,
                        32,
                        32,
                        32,
                        32,
                        32,
                        32,
                        32,
                        32,
                        32,
                        32,
                        32,
                        32,
                        32,
                        32,
                        32,
                        32));
            }
        });

        // polyphase synthesis
        add(new PCMSynthesis());
    }
}